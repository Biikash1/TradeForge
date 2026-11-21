package com.cryptotrading.service;

import com.cryptotrading.domain.OrderStatus;
import com.cryptotrading.domain.OrderType;
import com.cryptotrading.dto.TradeEvent;
import com.cryptotrading.exception.InvalidOrderException;
import com.cryptotrading.exception.OrderNotFoundException;
import com.cryptotrading.exception.UnauthorizedOrderException;
import com.cryptotrading.model.*;
import com.cryptotrading.repository.OrderRepository;
import com.cryptotrading.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final WalletService walletService;
    private final AssetService assetService;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    private static final Duration MAX_PRICE_AGE = Duration.ofSeconds(60);

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
    public Page<Order> getAllOrderOfUser(Long userId,
                                         OrderType orderType,
                                         String assetSymbol,
                                         int page, int size) {
        log.debug("Fetching orders for User {}, Type: {}, Symbol: {}, Page: {}", userId, orderType, assetSymbol, page);

        Pageable pageable = PageRequest.of(page, size);
        boolean hasOrderType = orderType != null;
        boolean hasAssetSymbol = assetSymbol != null && !assetSymbol.isBlank();

        if (hasOrderType && hasAssetSymbol) {
            return orderRepository.findByUserIdAndOrderTypeAndOrderItemCoinSymbolOrderByTimestampDesc(
                    userId, orderType, assetSymbol.toUpperCase(), pageable
            );
        }

        if (hasOrderType) {
            return orderRepository.findByUserIdAndOrderTypeOrderByTimestampDesc(userId, orderType, pageable);
        }

        if (hasAssetSymbol) {
            return orderRepository.findByUserIdAndOrderItemCoinSymbolOrderByTimestampDesc(
                    userId, assetSymbol.toUpperCase(), pageable
            );
        }

        return orderRepository.findByUserIdOrderByTimestampDesc(userId, pageable);
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
                    UUID.randomUUID().toString(),
                    savedOrder.getId(),
                    user.getId(),
                    coin.getId(),
                    savedOrder.getOrderType(),
                    quantity,
                    savedOrder.getPrice(),
                    Instant.now()
            );

            saveTradeOutboxEvent(event);
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

        BigDecimal reserved = asset.getReservedQuantity() != null ? asset.getReservedQuantity() : BigDecimal.ZERO;
        BigDecimal availableQty = asset.getQuantity().subtract(reserved);
        if (availableQty.compareTo(quantity) < 0) {
            log.warn("SELL failed for User {}: Insufficient available quantity. Available {}, trying to sell {}", user.getId(), availableQty, quantity);
            throw new InvalidOrderException("Insufficient available asset quantity (some holdings are reserved in pending orders)");
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
                    UUID.randomUUID().toString(),
                    savedOrder.getId(),
                    user.getId(),
                    coin.getId(),
                    savedOrder.getOrderType(),
                    quantity,
                    savedOrder.getPrice(),
                    Instant.now()
            );

            saveTradeOutboxEvent(event);
            return savedOrder;

        } catch (Exception e) {
            log.error("Failed to execute SELL Order for User {}. Reason: {}", user.getId(), e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    public Order processOrder(Coin coin, BigDecimal quantity, OrderType orderType, User user, BigDecimal targetPrice) {
        validateOrder(coin, quantity, orderType, user);

        if (orderType == OrderType.BUY || orderType == OrderType.SELL) {
            return orderType == OrderType.BUY ? buyAssets(coin, quantity, user) : sellAssets(coin, quantity, user);
        }

        if (targetPrice == null || targetPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOrderException("Target price is required for Limit and Stop-Loss orders");
        }

        boolean isAdvancedBuy = orderType == OrderType.LIMIT_BUY || orderType == OrderType.STOP_LOSS_BUY;
        BigDecimal reservedAmount = targetPrice.multiply(quantity);

        if (isAdvancedBuy) {
            walletService.reserveFunds(user, reservedAmount);
        } else {
            assetService.reserveAssetQuantity(user.getId(), coin.getId(), quantity);
        }

        OrderItem orderItem = createOrderItem(coin, quantity, null, null);
        Order order = Order.builder()
                .user(user)
                .orderType(orderType)
                .price(reservedAmount)
                .targetPrice(targetPrice)
                .status(OrderStatus.PENDING)
                .build();
        orderItem.setOrder(order);
        order.setOrderItem(orderItem);

        log.info("Created PENDING {} order for User {}. Target Price: {}, Reserved: {}",
                orderType, user.getId(), targetPrice, isAdvancedBuy ? "$" + reservedAmount : quantity + " " + coin.getSymbol());
        return orderRepository.save(order);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executePendingAdvanceOrder(Long orderId, Coin currentMarketCoin) {
        Order order = getOrderById(orderId);

        if (order.getStatus() != OrderStatus.PENDING) {
            return;
        }

        User user = order.getUser();
        BigDecimal quantity = order.getOrderItem().getQuantity();
        BigDecimal currentPrice = BigDecimal.valueOf(currentMarketCoin.getCurrentPrice());
        BigDecimal executionTotal = currentPrice.multiply(quantity);
        BigDecimal reservedAmount = order.getTargetPrice().multiply(quantity);
        boolean isAdvancedBuy = order.getOrderType() == OrderType.LIMIT_BUY
                || order.getOrderType() == OrderType.STOP_LOSS_BUY;

        order.setPrice(executionTotal);

        try {
            if (isAdvancedBuy) {
                order.getOrderItem().setBuyPrice(currentPrice);
                walletService.settleReservedBuyOrder(order, user, reservedAmount, executionTotal);

                Asset existingAsset = assetService.findAssetByUserIdAndCoinId(user.getId(), currentMarketCoin.getId());
                if (existingAsset == null) {
                    assetService.createAsset(user, currentMarketCoin, quantity);
                } else {
                    assetService.updateAsset(existingAsset.getId(), quantity);
                }
            } else {
                order.getOrderItem().setSellPrice(currentPrice);
                assetService.settleReservedSellAsset(user.getId(), currentMarketCoin.getId(), quantity);
                walletService.payOrder(order, user);
            }

            order.setStatus(OrderStatus.FILLED);
            Order savedOrder = orderRepository.save(order);
            log.info("Successfully executed advanced order ID {} for User {}", savedOrder.getId(), user.getId());

            TradeEvent event = new TradeEvent(
                    UUID.randomUUID().toString(),
                    savedOrder.getId(),
                    user.getId(),
                    currentMarketCoin.getId(),
                    savedOrder.getOrderType(),
                    quantity,
                    savedOrder.getPrice(),
                    Instant.now()
            );

            saveTradeOutboxEvent(event);

        } catch (InvalidOrderException | IllegalStateException e) {
            log.warn("Advanced order ID {} cancelled during execution: {}", order.getId(), e.getMessage());
            if (isAdvancedBuy) {
                walletService.releaseReservedFunds(user, reservedAmount);
            } else {
                assetService.releaseReservedAssetQuantity(user.getId(), currentMarketCoin.getId(), quantity);
            }
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
        }
    }

    @Override
    @Transactional
    public Order cancelOrder(Long orderId, User user) {
        Order order = getOrderById(orderId);

        if (!order.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedOrderException("You are not authorized to cancel this order");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Only PENDING orders can be cancelled. Current status: " + order.getStatus());
        }

        BigDecimal quantity = order.getOrderItem().getQuantity();
        boolean isAdvancedBuy = order.getOrderType() == OrderType.LIMIT_BUY
                || order.getOrderType() == OrderType.STOP_LOSS_BUY;

        if (isAdvancedBuy) {
            BigDecimal reservedAmount = order.getTargetPrice().multiply(quantity);
            walletService.releaseReservedFunds(user, reservedAmount);
        } else if (order.getOrderType() == OrderType.LIMIT_SELL || order.getOrderType() == OrderType.STOP_LOSS_SELL) {
            assetService.releaseReservedAssetQuantity(user.getId(), order.getOrderItem().getCoin().getId(), quantity);
        }

        order.setStatus(OrderStatus.CANCELLED);
        log.info("Order ID {} cancelled by User {}", orderId, user.getId());
        return orderRepository.save(order);
    }

    private OrderItem createOrderItem(Coin coin, BigDecimal quantity, BigDecimal buyPrice, BigDecimal sellPrice) {
        return OrderItem.builder()
                .coin(coin)
                .quantity(quantity)
                .buyPrice(buyPrice)
                .sellPrice(sellPrice)
                .build();
    }

    private void saveTradeOutboxEvent(TradeEvent event) {
        try {
            OutboxEvent outbox = OutboxEvent.builder()
                    .topic("trade-events")
                    .eventKey(event.eventId())
                    .payloadJson(objectMapper.writeValueAsString(event))
                    .published(false)
                    .createdAt(Instant.now())
                    .build();
            outboxEventRepository.save(outbox);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize TradeEvent for outbox", e);
        }
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
        if (coin.getLastUpdated() == null ||
                Duration.between(coin.getLastUpdated(), Instant.now()).compareTo(MAX_PRICE_AGE) > 0) {
            throw new InvalidOrderException("Market price for " + coin.getSymbol() + " is stale. Trading temporarily paused.");
        }
    }
}