package com.cryptotrading.dto;

import com.cryptotrading.domain.OrderStatus;
import com.cryptotrading.domain.OrderType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
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
}
