package com.cryptotrading.dto;

import com.cryptotrading.domain.OrderType;
import lombok.Data;

@Data
public class OrderRequest {

    private String coinId;
    private double quantity;
    private OrderType orderType;
}
