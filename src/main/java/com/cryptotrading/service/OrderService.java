package com.cryptotrading.service;

import com.cryptotrading.domain.OrderType;
import com.cryptotrading.model.Coin;
import com.cryptotrading.model.Order;
import com.cryptotrading.model.OrderItem;
import com.cryptotrading.model.User;

import java.util.List;

public interface OrderService {

    Order createOrder (User user, OrderItem orderItem, OrderType orderType);

    Order getOrderById(Long orderId) throws Exception;

    List<Order> getAllOrderOfUser(Long userId, OrderType orderType, String assetSymbol);

    Order processOrder(Coin coin, String quantity, OrderType orderType, User user) throws Exception;


}
