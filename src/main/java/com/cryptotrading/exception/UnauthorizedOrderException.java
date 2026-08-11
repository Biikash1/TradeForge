package com.cryptotrading.exception;

public class UnauthorizedOrderException extends RuntimeException {

    public UnauthorizedOrderException(String message) {
        super(message);
    }
}