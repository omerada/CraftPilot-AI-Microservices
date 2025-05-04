package com.craftpilot.llmservice.exception;

/**
 * Servis katmanı için temel exception sınıfı
 */
public class ServiceException extends RuntimeException {
    private final String errorCode;
    
    public ServiceException(String message) {
        super(message);
        this.errorCode = "GENERAL_SERVICE_ERROR";
    }
    
    public ServiceException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "GENERAL_SERVICE_ERROR";
    }
    
    public ServiceException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public ServiceException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
