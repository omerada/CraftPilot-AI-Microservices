package com.craftpilot.subscriptionservice.exception;

import org.springframework.http.HttpStatus;

public class PaymentProcessingException extends BaseException {
    private static final String ERROR_CODE = "PAYMENT_PROCESSING_ERROR";
    
    public PaymentProcessingException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, ERROR_CODE);
    }

    public PaymentProcessingException(String message, Throwable cause) {
        super(message, cause, HttpStatus.INTERNAL_SERVER_ERROR, ERROR_CODE);
    }
} 