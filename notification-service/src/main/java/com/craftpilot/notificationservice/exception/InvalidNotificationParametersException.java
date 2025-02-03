package com.craftpilot.notificationservice.exception;

public class InvalidNotificationParametersException extends RuntimeException {
    public InvalidNotificationParametersException(String message) {
        super(message);
    }

    public InvalidNotificationParametersException(String message, Throwable cause) {
        super(message, cause);
    }
} 