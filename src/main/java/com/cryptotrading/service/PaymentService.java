package com.cryptotrading.service;

import com.cryptotrading.domain.PaymentMethod;
import com.cryptotrading.dto.PaymentResponse;
import com.cryptotrading.model.PaymentOrder;
import com.cryptotrading.model.User;
import com.razorpay.RazorpayException;
import com.stripe.exception.StripeException;

public interface PaymentService {

    PaymentOrder createOrder(User user, Long amount,
                             PaymentMethod paymentMethod);


    PaymentOrder getPaymentOrderById(Long id) throws Exception;

    Boolean ProccedPaymentOrder(PaymentOrder paymentOrder,
                                     String paymentId) throws RazorpayException;

    PaymentResponse createRazorpayPaymentLink(User user, Long amount);

    PaymentResponse createStripePaymentLink(User user, Long amount, Long orderId) throws StripeException;
}
