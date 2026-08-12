package com.cryptotrading.exception;

public class VerificationCodeNotFoundException extends RuntimeException {

    public VerificationCodeNotFoundException(String message) {
        super(message);
    }
}