package com.craftpilot.contentservice.exception;

public class CreditNotFoundException extends ResourceNotFoundException {
    public CreditNotFoundException(String message) {
        super(message);
    }
} 