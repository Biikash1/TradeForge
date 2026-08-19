package com.cryptotrading.service;

import com.cryptotrading.domain.PaymentMethod;
import com.cryptotrading.dto.PaymentResponse;
import com.cryptotrading.model.PaymentOrder;
import com.cryptotrading.model.User;
import com.cryptotrading.model.Wallet;

import java.math.BigDecimal;

public interface PaymentService {

    PaymentOrder createOrder(User user,
                             BigDecimal amount,
                             PaymentMethod paymentMethod);


    PaymentOrder getPaymentOrderById(Long id);

    Wallet processPaymentAndCreditWallet(
            User user,
            Long orderId,
            String paymentId
    );

    boolean processRazorpayPayment(PaymentOrder paymentOrder,
                                     String paymentId);

    boolean processStripePayment(PaymentOrder paymentOrder,
            String sessionId);

    PaymentResponse createRazorpayPaymentLink(User user, BigDecimal amount, Long orderId);

    PaymentResponse createStripePaymentLink(User user, BigDecimal amount, Long orderId);
}
