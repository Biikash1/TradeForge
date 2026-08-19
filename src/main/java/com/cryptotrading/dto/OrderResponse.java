package com.cryptotrading.dto;

import com.cryptotrading.domain.OrderStatus;
import com.cryptotrading.domain.OrderType;
import com.cryptotrading.model.Order;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {

    private Long id;

    private Long userId;

    private String coinId;

    private String symbol;

    private BigDecimal quantity;

    private BigDecimal buyPrice;

    private BigDecimal sellPrice;

    private BigDecimal price;

    private OrderType orderType;

    private OrderStatus status;

    private LocalDateTime timestamp;

    public static OrderResponse from(Order order) {

        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUser().getId())
                .coinId(order.getOrderItem().getCoin().getId())
                .symbol(order.getOrderItem().getCoin().getSymbol())
                .quantity(order.getOrderItem().getQuantity())
                .buyPrice(order.getOrderItem().getBuyPrice())
                .sellPrice(order.getOrderItem().getSellPrice())
                .price(order.getPrice())
                .orderType(order.getOrderType())
                .status(order.getStatus())
                .timestamp(order.getTimestamp())
                .build();
    }
}
