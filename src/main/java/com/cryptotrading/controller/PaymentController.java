package com.cryptotrading.controller;

import com.cryptotrading.domain.PaymentMethod;
import com.cryptotrading.dto.PaymentResponse;
import com.cryptotrading.model.PaymentOrder;
import com.cryptotrading.model.User;
import com.cryptotrading.service.PaymentService;
import com.cryptotrading.service.UserService;
import com.stripe.exception.StripeException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final UserService userService;

    private final PaymentService paymentService;

    @PostMapping("/{paymentMethod}/amount/{amount}")
    public ResponseEntity<PaymentResponse> paymentHandler(
            @PathVariable PaymentMethod paymentMethod,
            @PathVariable Long amount,
            @RequestHeader("Authorization") String jwt) throws StripeException {

        User user = userService.findUserProfileByJwt(jwt);

        PaymentResponse paymentResponse;

        PaymentOrder order = paymentService.createOrder(user, amount, paymentMethod);

        if(paymentMethod.equals(PaymentMethod.RAZORPAY)) {
            paymentResponse = paymentService.createRazorpayPaymentLink(user, amount);
        }else {
            paymentResponse = paymentService.createStripePaymentLink(user, amount, order.getId());
        }
        return new ResponseEntity<>(paymentResponse, HttpStatus.CREATED);
    }
}
