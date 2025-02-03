package com.craftpilot.aiquestionservice.exception;

public class AIModelNotFoundException extends RuntimeException {
    public AIModelNotFoundException(String message) {
        super(message);
    }

    public AIModelNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
} 