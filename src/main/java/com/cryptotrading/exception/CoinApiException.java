package com.cryptotrading.exception;

public class CoinApiException extends RuntimeException{

    public CoinApiException(String message) {
        super(message);
    }

    public CoinApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
