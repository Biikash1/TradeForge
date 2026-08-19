package com.cryptotrading.service;

import com.cryptotrading.domain.OrderStatus;
import com.cryptotrading.domain.OrderType;
import com.cryptotrading.exception.InvalidOrderException;
import com.cryptotrading.exception.OrderNotFoundException;
import com.cryptotrading.model.*;
import com.cryptotrading.repository.OrderItemRepository;
import com.cryptotrading.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final WalletService walletService;
    private final AssetService assetService;

    @Override
    @Transactional
    public Order createOrder(User user, OrderItem orderItem, OrderType orderType) {
        BigDecimal totalPrice =
                orderItem.getCoin()
                        .getCurrentPrice()
                        .multiply(orderItem.getQuantity());

        Order order = Order.builder()
                .user(user)
                .orderType(orderType)
                .price(totalPrice)
                .status(OrderStatus.PENDING)
                .build();

        orderItem.setOrder(order);
        order.setOrderItem(orderItem);
        return orderRepository.save(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Order getOrderById(Long orderId){

        return orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new OrderNotFoundException(
                                "order not found with id:" + orderId
                        )
                );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getAllOrderOfUser(Long userId, OrderType orderType, String assetSymbol) {
        boolean hasOrderType = orderType != null;

        boolean hasAssetSymbol =
                assetSymbol != null &&
                        !assetSymbol.isBlank();

        if (hasOrderType && hasAssetSymbol) {
            return orderRepository
                    .findByUserIdAndOrderTypeAndOrderItemCoinSymbolOrderByTimestampDesc(
                            userId,
                            orderType,
                            assetSymbol.toUpperCase()
                    );
        }

        if (hasOrderType) {
            return orderRepository
                    .findByUserIdAndOrderTypeOrderByTimestampDesc(
                            userId,
                            orderType
                    );
        }

        if (hasAssetSymbol) {
            return orderRepository
                    .findByUserIdAndOrderItemCoinSymbolOrderByTimestampDesc(
                            userId,
                            assetSymbol.toUpperCase()
                    );
        }

        return orderRepository
                .findByUserIdOrderByTimestampDesc(userId);
    }



    @Transactional
    public Order buyAssets(Coin coin, BigDecimal quantity, User user){
        BigDecimal buyPrice =
                coin.getCurrentPrice();

        OrderItem orderItem =
                createOrderItem(
                        coin,
                        quantity,
                        buyPrice,
                        null
                );

        Order order =
                createOrder(
                        user,
                        orderItem,
                        OrderType.BUY
                );

        /*
         * BUY:
         * User wallet -> exchange
         */
        walletService.payOrder(
                order,
                user
        );

        /*
         * Update user's crypto asset.
         */
        Asset existingAsset =
                assetService.findAssetByUserIdAndCoinId(
                        user.getId(),
                        coin.getId()
                );

        if (existingAsset == null) {

            assetService.createAsset(
                    user,
                    coin,
                    quantity
            );

        } else {

            assetService.updateAsset(
                    existingAsset.getId(),
                    quantity
            );
        }

        order.setStatus(OrderStatus.FILLED);

        return orderRepository.save(order);
    }

    @Transactional
    public Order sellAssets(
            Coin coin,
            BigDecimal quantity,
            User user)  {

        Asset asset =
                assetService.findAssetByUserIdAndCoinId(
                        user.getId(),
                        coin.getId()
                );

        if (asset == null) {
            throw new InvalidOrderException(
                    "You do not own this asset"
            );
        }

        if (asset.getQuantity().compareTo(quantity) < 0) {
            throw new InvalidOrderException(
                    "Insufficient asset quantity"
            );
        }

        BigDecimal sellPrice =
                coin.getCurrentPrice();

        BigDecimal buyPrice =
                asset.getBuyPrice();

        OrderItem orderItem =
                createOrderItem(
                        coin,
                        quantity,
                        buyPrice,
                        sellPrice
                );

        Order order =
                createOrder(
                        user,
                        orderItem,
                        OrderType.SELL
                );


        /*
         * SELL:
         * Crypto asset -> exchange
         * Money -> user's wallet
         */
        walletService.payOrder(
                order,
                user
        );

        Asset updatedAsset =
                assetService.updateAsset(
                        asset.getId(),
                        quantity.negate()
                );

        /*
         * Delete the asset only when
         * quantity becomes exactly zero.
         */
        if (updatedAsset.getQuantity().signum() == 0) {

            assetService.deleteAsset(
                    updatedAsset.getId()
            );
        }

        order.setStatus(OrderStatus.FILLED);

        return orderRepository.save(order);
    }

    @Transactional
    public Order processOrder(
            Coin coin,
            BigDecimal quantity,
            OrderType orderType,
            User user) {

        validateOrder(coin, quantity, orderType, user);

        return switch (orderType) {

            case BUY -> buyAssets(
                    coin,
                    quantity,
                    user
            );

            case SELL -> sellAssets(
                    coin,
                    quantity,
                    user
            );
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

    private void validateOrder(
            Coin coin,
            BigDecimal quantity,
            OrderType orderType,
            User user) {

        if (user == null) {
            throw new InvalidOrderException(
                    "User is required"
            );
        }

        if (coin == null) {
            throw new InvalidOrderException(
                    "Coin is required"
            );
        }

        if (quantity == null ||
                quantity.signum() <= 0) {

            throw new InvalidOrderException(
                    "Quantity must be greater than zero"
            );
        }

        if (orderType == null) {
            throw new InvalidOrderException(
                    "Order type is required"
            );
        }

        if (coin.getCurrentPrice() == null ||
                coin.getCurrentPrice().signum() <= 0) {

            throw new InvalidOrderException(
                    "Coin price is not available"
            );
        }
    }

}
