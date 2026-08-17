package com.example.transactionlab.mybatis;

public class OrderRejectedException extends Exception {
    public OrderRejectedException(String message) {
        super(message);
    }
}
