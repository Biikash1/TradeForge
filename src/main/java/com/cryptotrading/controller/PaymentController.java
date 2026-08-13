package com.cryptotrading.controller;

import com.cryptotrading.domain.PaymentMethod;
import com.cryptotrading.dto.PaymentResponse;
import com.cryptotrading.exception.InvalidPaymentException;
import com.cryptotrading.model.PaymentOrder;
import com.cryptotrading.model.User;
import com.cryptotrading.service.PaymentService;
import com.cryptotrading.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final UserService userService;

    private final PaymentService paymentService;

    @PostMapping("/{paymentMethod}/amount/{amount}")
    public ResponseEntity<PaymentResponse> paymentHandler(
            @PathVariable PaymentMethod paymentMethod,
            @PathVariable BigDecimal amount,
            @RequestHeader("Authorization") String jwt) {

        User user = userService.findUserProfileByJwt(jwt);

        // Create internal payment order first.
         // Status = PENDING

        PaymentOrder order = paymentService.createOrder(
                user,
                amount,
                paymentMethod
        );

        Long orderId = order.getId();

        PaymentResponse paymentResponse;

        if (paymentMethod == PaymentMethod.RAZORPAY) {
            paymentResponse =
                    paymentService.createRazorpayPaymentLink(
                            user,
                            amount,
                            orderId
                    );

        } else if (paymentMethod == PaymentMethod.STRIPE) {
            paymentResponse =
                    paymentService.createStripePaymentLink(
                            user,
                            amount,
                            orderId
                    );

        } else {
            throw new InvalidPaymentException(
                    "Unsupported payment method: " + paymentMethod
            );
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(paymentResponse);
    }

}
