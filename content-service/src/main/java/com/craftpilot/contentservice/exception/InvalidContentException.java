package com.craftpilot.contentservice.exception;

public class InvalidContentException extends ValidationException {
    public InvalidContentException(String message) {
        super(message);
    }

    public InvalidContentException(String message, Throwable cause) {
        super(message, cause);
    }
} 