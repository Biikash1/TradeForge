package com.cryptotrading.exception;

public class PaymentOrderNotFoundException extends RuntimeException {

    public PaymentOrderNotFoundException(String message) {
        super(message);
    }
}