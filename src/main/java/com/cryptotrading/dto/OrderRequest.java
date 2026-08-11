package com.cryptotrading.dto;

import com.cryptotrading.domain.OrderType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderRequest {

    @NotBlank(message = "Coin ID is required")
    private String coinId;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.00000001")
    private BigDecimal quantity;

    @NotNull(message = "Order type is required")
    private OrderType orderType;
}
