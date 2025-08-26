package com.cosmetics.server.exception;

public class OtpCooldownException extends RuntimeException {
    public OtpCooldownException(String message) {
        super(message);
    }
}
