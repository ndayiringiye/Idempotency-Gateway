package com.igirepay.gateway.exception;

import lombok.Getter;

@Getter
public class IdempotencyException extends RuntimeException {

    private final int statusCode;

    public IdempotencyException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }
}