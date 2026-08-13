package com.cryptotrading.model;

import com.cryptotrading.domain.PaymentMethod;
import com.cryptotrading.domain.PaymentOrderStatus;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Data;

@Data
public class PaymentOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private  Long amount;

    private PaymentOrderStatus status;

    private PaymentMethod paymentMethod;

    @ManyToOne
    private User user;
}
