package com.craftpilot.llmservice.exception;

/**
 * AI servisi ile ilgili hatalar için özel exception
 */
public class AIServiceException extends ServiceException {
    public AIServiceException(String message) {
        super("AI_SERVICE_ERROR", message);
    }

    public AIServiceException(String message, Throwable cause) {
        super("AI_SERVICE_ERROR", message, cause);
    }
    
    public AIServiceException(String errorCode, String message) {
        super(errorCode, message);
    }
}