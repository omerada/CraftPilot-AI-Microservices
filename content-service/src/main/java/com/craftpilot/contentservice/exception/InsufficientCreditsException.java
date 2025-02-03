package com.craftpilot.contentservice.exception;

public class InsufficientCreditsException extends ValidationException {
    public InsufficientCreditsException(String message) {
        super(message);
    }
} 