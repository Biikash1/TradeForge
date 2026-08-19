package com.cryptotrading.repository;

import com.cryptotrading.domain.OrderType;
import com.cryptotrading.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserIdOrderByTimestampDesc(Long userId);

    List<Order> findByUserIdAndOrderTypeOrderByTimestampDesc(
            Long userId,
            OrderType orderType
    );

    List<Order> findByUserIdAndOrderItemCoinSymbolOrderByTimestampDesc(
            Long userId,
            String symbol
    );

    List<Order> findByUserIdAndOrderTypeAndOrderItemCoinSymbolOrderByTimestampDesc(
            Long userId,
            OrderType orderType,
            String symbol
    );
}
