package com.cryptotrading.exception;

public class GeminiApiException extends RuntimeException {
    private final int statusCode;

    public GeminiApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}