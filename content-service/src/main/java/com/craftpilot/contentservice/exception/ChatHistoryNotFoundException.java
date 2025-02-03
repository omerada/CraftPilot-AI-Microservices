package com.craftpilot.contentservice.exception;

public class ChatHistoryNotFoundException extends ResourceNotFoundException {
    public ChatHistoryNotFoundException(String message) {
        super(message);
    }

    public ChatHistoryNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
} 