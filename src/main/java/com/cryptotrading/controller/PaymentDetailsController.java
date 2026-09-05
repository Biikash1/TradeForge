package com.cryptotrading.controller;

import com.cryptotrading.dto.PaymentDetailsRequest;
import com.cryptotrading.model.PaymentDetails;
import com.cryptotrading.model.User;
import com.cryptotrading.service.PaymentDetailsService;
import com.cryptotrading.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payment-details")
@RequiredArgsConstructor
public class PaymentDetailsController {

    private final UserService userService;
    private final PaymentDetailsService paymentDetailsService;

    @PostMapping
    public ResponseEntity<PaymentDetails> addPaymentDetails(
            @Valid @RequestBody PaymentDetailsRequest request,
            @RequestHeader("Authorization") String jwt) {

        User user = userService.findUserProfileByJwt(jwt);
        PaymentDetails paymentDetails = paymentDetailsService.addPaymentDetails(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentDetails);
    }

    @GetMapping
    public ResponseEntity<List<PaymentDetails>> getUsersPaymentDetails(
            @RequestHeader("Authorization") String jwt) {

        User user = userService.findUserProfileByJwt(jwt);
        List<PaymentDetails> paymentDetailsList = paymentDetailsService.getUsersPaymentDetails(user);
        return ResponseEntity.ok(paymentDetailsList);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deletePaymentDetails(
            @PathVariable Long id,
            @RequestHeader("Authorization") String jwt) {

        User user = userService.findUserProfileByJwt(jwt);
        paymentDetailsService.deletePaymentDetails(id, user);
        return ResponseEntity.ok("Bank account removed successfully");
    }
}