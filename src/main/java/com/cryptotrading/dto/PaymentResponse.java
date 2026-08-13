package com.cryptotrading.dto;

import lombok.Data;

@Data
public class PaymentResponse {

    private String paymentId;
    private String payment_url;
}
