package com.cryptotrading.controller;

import com.cryptotrading.model.PaymentDetails;
import com.cryptotrading.model.User;
import com.cryptotrading.service.PaymentDetailsService;
import com.cryptotrading.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/paymentDetails")
@RequiredArgsConstructor
public class PaymentDetailsController {

    private final  UserService userService;

    private final PaymentDetailsService paymentDetailsService;

    @PostMapping("/payment-details")
    public ResponseEntity<PaymentDetails> addPaymentDetails(
            @RequestBody PaymentDetails paymentDetailRequest,
            @RequestHeader("Authorization") String jwt) throws Exception {

        User user = userService.findUserProfileByJwt(jwt);

        PaymentDetails paymentDetails = paymentDetailsService.addPaymentDetails(
                paymentDetailRequest.getAccountNumber(),
                paymentDetailRequest.getAccountHolderName(),
                paymentDetailRequest.getIfsc(),
                paymentDetailRequest.getBankName(),
                user
        );
        return new ResponseEntity<>(paymentDetails, HttpStatus.CREATED);

    }

    @GetMapping("/payment-details")
    public ResponseEntity<PaymentDetails> addUsersPaymentDetails(
            @RequestHeader("Authorization") String jwt) throws Exception {

        User user = userService.findUserProfileByJwt(jwt);

        PaymentDetails paymentDetails = paymentDetailsService
                .getUsersPaymentDetails(user);

        return new ResponseEntity<>(paymentDetails, HttpStatus.CREATED);

    }

}
