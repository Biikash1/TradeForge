package com.cryptotrading.service;

import com.cryptotrading.domain.OrderStatus;
import com.cryptotrading.domain.OrderType;
import com.cryptotrading.model.*;
import com.cryptotrading.repository.OrderItemRepository;
import com.cryptotrading.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final WalletService walletService;
    private final OrderItemRepository orderItemRepository;
    private final AssetService assetService;

    @Override
    public Order createOrder(User user, OrderItem orderItem, OrderType orderType) {
        double price = orderItem.getCoin().getCurrentPrice() *
                orderItem.getQuantity();

        Order order = new Order();
        order.setUser(user);
        order.setOrderItem(orderItem);
        order.setOrderType(orderType);
        order.setPrice(BigDecimal.valueOf(price));
        order.setTimestamp(LocalDateTime.now());
        order.setStatus(OrderStatus.PENDING);
        return orderRepository.save(order);
    }

    @Override
    public Order getOrderById(Long orderId) throws Exception {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new Exception("order not found with id:" + orderId));
    }

    @Override
    public List<Order> getAllOrderOfUser(Long userId, OrderType orderType, String assetSymbol) {
        return orderRepository.findByUserId(userId);
    }

    private OrderItem createOrderItem(Coin coin, double quantity, double buyPrice, double sellPrice) {
        OrderItem orderItem = new OrderItem();
        orderItem.setCoin(coin);
        orderItem.setQuantity(quantity);
        orderItem.setBuyPrice(buyPrice);
        orderItem.setSellPrice(sellPrice);
        return orderItemRepository.save(orderItem);
    }

    @Transactional
    public Order buyAssets(Coin coin, double quantity, User user) throws Exception {
        if (quantity <= 0) {
            throw new Exception("Quantity must be greater than zero");
        }
        double buyPrice = coin.getCurrentPrice();

        OrderItem orderItem = createOrderItem(coin, quantity, buyPrice, 0);

        Order order = createOrder(user, orderItem, OrderType.BUY);
        orderItem.setOrder(order);
        orderItemRepository.save(orderItem);

        // Deduct money from wallet
        walletService.payOrderPayment(order, user);
        order.setStatus(OrderStatus.SUCCESS);
        order.setOrderType(OrderType.BUY);
        Order savedOrder = orderRepository.save(order);

        //Update User's assets
        Asset oldAsset = assetService.findAssetByUserIdAndCoinId(
               user.getId(),
                coin.getId()
        );

        if (oldAsset == null) {
            assetService.createAsset(user, orderItem.getCoin(), orderItem.getQuantity());
        } else {
            assetService.updateAsset(oldAsset.getId(), quantity);
        }
        return savedOrder;
    }

    @Transactional
    public Order sellAssets(
            Coin coin,
            double quantity,
            User user) throws Exception {

        if (quantity <= 0) {
            throw new Exception("Quantity must be greater than zero");
        }

        Asset assetsToSell =
                assetService.findAssetByUserIdAndCoinId(
                        user.getId(),
                        coin.getId()
                );

        // Check asset before accessing it
        if (assetsToSell == null) {
            throw new Exception("Asset not found");
        }

        // Check quantity before creating order
        if (assetsToSell.getQuantity() < quantity) {
            throw new Exception("Insufficient quantity to sell");
        }

        double sellPrice = coin.getCurrentPrice();
        double buyPrice = assetsToSell.getBuyPrice();

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

        orderItem.setOrder(order);
        orderItemRepository.save(orderItem);

        // Credit wallet
        walletService.payOrderPayment(order, user);

        order.setStatus(OrderStatus.SUCCESS);

        Order savedOrder = orderRepository.save(order);

        // Decrease asset quantity
        Asset updatedAsset =
                assetService.updateAsset(
                        assetsToSell.getId(),
                        -quantity
                );

        // Remove almost-empty asset
        if (updatedAsset.getQuantity() * coin.getCurrentPrice() <= 1) {
            assetService.deleteAsset(updatedAsset.getId());
        }

        return savedOrder;
    }

    @Transactional
    public Order processOrder(
            Coin coin,
            double quantity,
            OrderType orderType,
            User user) throws Exception {

        if (orderType == null) {
            throw new Exception("Order type cannot be null");
        }

        if (orderType == OrderType.BUY) {
            return buyAssets(coin, quantity, user);
        }

        if (orderType == OrderType.SELL) {
            return sellAssets(coin, quantity, user);
        }

        throw new Exception("Invalid order type");
    }

}
