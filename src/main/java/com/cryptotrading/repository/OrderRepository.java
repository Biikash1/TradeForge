package com.cryptotrading.repository;

import com.cryptotrading.domain.OrderStatus;
import com.cryptotrading.domain.OrderType;
import com.cryptotrading.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByUserIdOrderByTimestampDesc(Long userId, Pageable pageable);

    Page<Order> findByUserIdAndOrderTypeOrderByTimestampDesc(
            Long userId,
            OrderType orderType,
            Pageable pageable
    );

    Page<Order> findByUserIdAndOrderItemCoinSymbolOrderByTimestampDesc(
            Long userId,
            String symbol,
            Pageable pageable
    );

    Page<Order> findByUserIdAndOrderTypeAndOrderItemCoinSymbolOrderByTimestampDesc(
            Long userId,
            OrderType orderType,
            String symbol,
            Pageable pageable
    );

    List<Order> findByStatusAndOrderTypeIn(
            OrderStatus status,
            List<OrderType> orderTypes
    );
}
