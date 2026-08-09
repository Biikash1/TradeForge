package com.cryptotrading.service;

import com.cryptotrading.domain.OrderStatus;
import com.cryptotrading.domain.OrderType;
import com.cryptotrading.model.Coin;
import com.cryptotrading.model.Order;
import com.cryptotrading.model.OrderItem;
import com.cryptotrading.model.User;
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
public class OrderServiceImpl implements  OrderService{

    private final OrderRepository orderRepository;
    private final WalletService walletService;
    private final OrderItemRepository orderItemRepository;

    @Override
    public Order createOrder(User user, OrderItem orderItem, OrderType orderType) {
       double price = orderItem.getCoin().getCurrentPrice()*orderItem.getQuantity();

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
                .orElseThrow(() -> new Exception("order not found"));
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
        if(quantity <= 0) {
            throw new Exception("Quantity should not be zero");
        }
        double buyPrice = coin.getCurrentPrice();

        OrderItem orderItem = createOrderItem(coin, quantity, buyPrice, 0);

        Order order = createOrder(user, orderItem, OrderType.BUY);
        orderItem.setOrder(order);

        walletService.payOrderPayment(order, user);
        order.setStatus(OrderStatus.SUCCESS);
        order.setOrderType(OrderType.BUY);
        Order savedOrder = orderRepository.save(order);

        //Create assets

        return savedOrder;
    }

    @Transactional
    public Order sellAssets(Coin coin, double quantity, User user) throws Exception {
        if(quantity < 0) {
            throw new Exception("Quantity should not be zero");
        }
        double sellPrice = coin.getCurrentPrice();

        double buyPrice = assetsToSell.getPrice();

        OrderItem orderItem = createOrderItem(coin, quantity, buyPrice, sellPrice);

        Order order = createOrder(user, orderItem, OrderType.SELL);
        orderItem.setOrder(order);

        if(assetsToSell.getQuantity() >= quantity) {
            order.setStatus(OrderStatus.SUCCESS);
            order.setOrderType(OrderType.SELL);
            Order savedOrder = orderRepository.save(order);
            walletService.payOrderPayment(order, user);

            Asset updateAsset = assetService.updateAsset(assetsToSell.getId(), -quantity);
            if(updateAsset.getQuantity()*coin.getCurrentPrice() <= 1) {
                assetService.deleteAsset(updateAsset.getId);
            }
            return savedOrder;
        }else {
            throw new Exception("Insufficient quantity to sell");
        }
    }

    @Override
    @Transactional
    public Order processOrder(Coin coin, double quantity, OrderType orderType, User user) throws Exception {

        if(orderType.equals(OrderType.BUY)) {
            return buyAssets(coin, quantity, user);
        }else if(orderType.equals(OrderType.SELL))  {
            return sellAssets(coin, quantity, user);
        }
        throw  new Exception("Invalid order Type");
    }
}
