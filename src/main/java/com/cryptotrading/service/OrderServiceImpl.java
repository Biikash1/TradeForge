package com.cryptotrading.service;

import com.cryptotrading.domain.OrderStatus;
import com.cryptotrading.domain.OrderType;
import com.cryptotrading.dto.TradeEvent;
import com.cryptotrading.exception.InvalidOrderException;
import com.cryptotrading.exception.OrderNotFoundException;
import com.cryptotrading.model.*;
import com.cryptotrading.repository.OrderRepository;
import com.cryptotrading.service.kafka.TradeEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final WalletService walletService;
    private final AssetService assetService;
    private final TradeEventProducer tradeEventProducer;

    @Override
    @Transactional
    public Order createOrder(User user, OrderItem orderItem, OrderType orderType) {
        BigDecimal coinPrice = BigDecimal.valueOf(orderItem.getCoin().getCurrentPrice());
        BigDecimal totalPrice = coinPrice.multiply(orderItem.getQuantity());

        Order order = Order.builder()
                .user(user)
                .orderType(orderType)
                .price(totalPrice)
                .status(OrderStatus.PENDING)
                .build();

        orderItem.setOrder(order);
        order.setOrderItem(orderItem);
        Order savedOrder = orderRepository.save(order);

        log.info("Created PENDING {} order for User {}. Order ID: {}", orderType, user.getId(), savedOrder.getId());
        return savedOrder;
    }

    @Override
    @Transactional(readOnly = true)
    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.warn("getOrderById failed: Order ID {} not found.", orderId);
                    return new OrderNotFoundException("Order not found with id: " + orderId);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getAllOrderOfUser(Long userId, OrderType orderType, String assetSymbol) {
        log.debug("Fetching orders for User {}, Type: {}, Symbol: {}", userId, orderType, assetSymbol);

        boolean hasOrderType = orderType != null;
        boolean hasAssetSymbol = assetSymbol != null && !assetSymbol.isBlank();

        if (hasOrderType && hasAssetSymbol) {
            return orderRepository.findByUserIdAndOrderTypeAndOrderItemCoinSymbolOrderByTimestampDesc(
                    userId, orderType, assetSymbol.toUpperCase()
            );
        }

        if (hasOrderType) {
            return orderRepository.findByUserIdAndOrderTypeOrderByTimestampDesc(userId, orderType);
        }

        if (hasAssetSymbol) {
            return orderRepository.findByUserIdAndOrderItemCoinSymbolOrderByTimestampDesc(
                    userId, assetSymbol.toUpperCase()
            );
        }

        return orderRepository.findByUserIdOrderByTimestampDesc(userId);
    }

    @Transactional
    public Order buyAssets(Coin coin, BigDecimal quantity, User user) {
        log.info("Initiating BUY execution for User {}, Coin: {}, Quantity: {}", user.getId(), coin.getSymbol(), quantity);

        BigDecimal buyPrice = BigDecimal.valueOf(coin.getCurrentPrice());
        OrderItem orderItem = createOrderItem(coin, quantity, buyPrice, null);
        Order order = createOrder(user, orderItem, OrderType.BUY);

        try {
            walletService.payOrder(order, user);

            Asset existingAsset = assetService.findAssetByUserIdAndCoinId(user.getId(), coin.getId());
            if (existingAsset == null) {
                assetService.createAsset(user, coin, quantity);
            } else {
                assetService.updateAsset(existingAsset.getId(), quantity);
            }

            order.setStatus(OrderStatus.FILLED);
            Order savedOrder = orderRepository.save(order);

            log.info("Successfully filled BUY Order {} for User {}", savedOrder.getId(), user.getId());

            TradeEvent event = new TradeEvent(
                    java.util.UUID.randomUUID().toString(),
                    savedOrder.getId(),
                    user.getId(),
                    coin.getId(),
                    savedOrder.getOrderType(),
                    quantity,
                    savedOrder.getPrice(),
                    Instant.now()
            );

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    tradeEventProducer.sendTradeEvent(event);
                }
            });
            return savedOrder;

        } catch (Exception e) {
            log.error("Failed to execute BUY Order for User {}. Reason: {}", user.getId(), e.getMessage());
            throw e;
        }
    }

    @Transactional
    public Order sellAssets(Coin coin, BigDecimal quantity, User user) {
        log.info("Initiating SELL execution for User {}, Coin: {}, Quantity: {}", user.getId(), coin.getSymbol(), quantity);

        Asset asset = assetService.findAssetByUserIdAndCoinId(user.getId(), coin.getId());

        if (asset == null) {
            log.warn("SELL failed for User {}: User does not own asset {}", user.getId(), coin.getSymbol());
            throw new InvalidOrderException("You do not own this asset");
        }

        if (asset.getQuantity().compareTo(quantity) < 0) {
            log.warn("SELL failed for User {}: Insufficient quantity. Owns {}, trying to sell {}", user.getId(), asset.getQuantity(), quantity);
            throw new InvalidOrderException("Insufficient asset quantity");
        }

        BigDecimal sellPrice = BigDecimal.valueOf(coin.getCurrentPrice());
        BigDecimal buyPrice = asset.getBuyPrice();

        OrderItem orderItem = createOrderItem(coin, quantity, buyPrice, sellPrice);
        Order order = createOrder(user, orderItem, OrderType.SELL);

        try {
            walletService.payOrder(order, user);

            Asset updatedAsset = assetService.updateAsset(asset.getId(), quantity.negate());
            if (updatedAsset.getQuantity().signum() == 0) {
                assetService.deleteAsset(updatedAsset.getId());
            }

            order.setStatus(OrderStatus.FILLED);
            Order savedOrder = orderRepository.save(order);

            log.info("Successfully filled SELL Order {} for User {}", savedOrder.getId(), user.getId());

            TradeEvent event = new TradeEvent(
                    java.util.UUID.randomUUID().toString(),
                    savedOrder.getId(),
                    user.getId(),
                    coin.getId(),
                    savedOrder.getOrderType(),
                    quantity,
                    savedOrder.getPrice(),
                    Instant.now()
            );

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    tradeEventProducer.sendTradeEvent(event);
                }
            });
            return savedOrder;

        } catch (Exception e) {
            log.error("Failed to execute SELL Order for User {}. Reason: {}", user.getId(), e.getMessage());
            throw e;
        }
    }

    @Transactional
    public Order processOrder(Coin coin, BigDecimal quantity, OrderType orderType, User user) {
        validateOrder(coin, quantity, orderType, user);

        return switch (orderType) {
            case BUY -> buyAssets(coin, quantity, user);
            case SELL -> sellAssets(coin, quantity, user);
        };
    }

    private OrderItem createOrderItem(Coin coin, BigDecimal quantity, BigDecimal buyPrice, BigDecimal sellPrice) {
        return OrderItem.builder()
                .coin(coin)
                .quantity(quantity)
                .buyPrice(buyPrice)
                .sellPrice(sellPrice)
                .build();
    }

    private void validateOrder(Coin coin, BigDecimal quantity, OrderType orderType, User user) {
        if (user == null) {
            log.warn("Order validation failed: User is null");
            throw new InvalidOrderException("User is required");
        }
        if (coin == null) {
            log.warn("Order validation failed for User {}: Coin is null", user.getId());
            throw new InvalidOrderException("Coin is required");
        }
        if (quantity == null || quantity.signum() <= 0) {
            log.warn("Order validation failed for User {}: Quantity must be greater than zero", user.getId());
            throw new InvalidOrderException("Quantity must be greater than zero");
        }
        if (orderType == null) {
            log.warn("Order validation failed for User {}: OrderType is null", user.getId());
            throw new InvalidOrderException("Order type is required");
        }
        if (coin.getCurrentPrice() <= 0) {
            log.warn("Order validation failed for User {}: Coin {} has invalid price {}", user.getId(), coin.getSymbol(), coin.getCurrentPrice());
            throw new InvalidOrderException("Coin price is not available");
        }
    }
}