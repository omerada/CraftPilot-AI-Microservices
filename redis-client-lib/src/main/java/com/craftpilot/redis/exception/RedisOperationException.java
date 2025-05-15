package com.craftpilot.redis.exception;

/**
 * Redis operasyonları sırasında oluşan hataları temsil eder
 */
public class RedisOperationException extends RuntimeException {
    
    public RedisOperationException(String message) {
        super(message);
    }
    
    public RedisOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
