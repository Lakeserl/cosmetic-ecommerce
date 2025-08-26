package com.cosmetics.server.exception;

public class OtpBlockedException extends RuntimeException {
    public OtpBlockedException(String message) {
        super(message);
    }
}
