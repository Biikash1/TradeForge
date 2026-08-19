package com.cryptotrading.exception;

public class InvalidOrderException extends RuntimeException{

    public InvalidOrderException(String message) {
        super(message);
    }
}
