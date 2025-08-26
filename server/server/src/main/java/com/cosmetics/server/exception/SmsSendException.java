package com.cosmetics.server.exception;

public class SmsSendException extends RuntimeException {
    public SmsSendException(String message) {
        super(message);
    }
}
