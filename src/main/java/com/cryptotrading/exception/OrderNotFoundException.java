package com.cryptotrading.exception;

public class OrderNotFoundException  extends RuntimeException{

    public OrderNotFoundException(String message) {
        super(message);
    }
}
