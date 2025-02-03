package com.craftpilot.subscriptionservice.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedAccessException extends BaseException {
    private static final String ERROR_CODE = "UNAUTHORIZED_ACCESS";
    
    public UnauthorizedAccessException(String message) {
        super(message, HttpStatus.FORBIDDEN, ERROR_CODE);
    }
} 