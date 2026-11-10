package com.cryptotrading.service;

import com.cryptotrading.domain.OrderType;
import com.cryptotrading.model.Coin;
import com.cryptotrading.model.Order;
import com.cryptotrading.model.OrderItem;
import com.cryptotrading.model.User;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;

public interface OrderService {

    Order createOrder (User user, OrderItem orderItem, OrderType orderType);

    Order getOrderById(Long orderId);

    Page<Order> getAllOrderOfUser(Long userId, OrderType orderType, String assetSymbol, int page, int size);

    Order processOrder(Coin coin, BigDecimal quantity, OrderType orderType, User user,BigDecimal targetPrice);

    void executePendingAdvanceOrder(Long orderId, Coin currentMarketCoin);


}
