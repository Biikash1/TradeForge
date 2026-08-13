package com.cryptotrading.service;

import com.cryptotrading.domain.PaymentMethod;
import com.cryptotrading.dto.PaymentResponse;
import com.cryptotrading.model.PaymentOrder;
import com.cryptotrading.model.User;
import com.razorpay.RazorpayException;
import com.stripe.exception.StripeException;

import java.math.BigDecimal;

public interface PaymentService {

    PaymentOrder createOrder(User user,
                             BigDecimal amount,
                             PaymentMethod paymentMethod);


    PaymentOrder getPaymentOrderById(Long id);

    boolean processRazorpayPayment(PaymentOrder paymentOrder,
                                     String paymentId) throws RazorpayException;

    boolean processStripePayment(PaymentOrder paymentOrder,
            String sessionId) throws StripeException;

    PaymentResponse createRazorpayPaymentLink(User user, BigDecimal amount, Long orderId) throws RazorpayException;

    PaymentResponse createStripePaymentLink(User user, BigDecimal amount, Long orderId) throws StripeException;
}
