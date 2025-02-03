package com.craftpilot.aiquestionservice.exception;

public class QuestionHistoryNotFoundException extends RuntimeException {
    public QuestionHistoryNotFoundException(String message) {
        super(message);
    }

    public QuestionHistoryNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
} 