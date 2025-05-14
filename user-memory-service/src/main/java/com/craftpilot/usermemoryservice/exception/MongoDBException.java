package com.craftpilot.usermemoryservice.exception;

public class MongoDBException extends RuntimeException {
    
    public MongoDBException(String message) {
        super(message);
    }
    
    public MongoDBException(String message, Throwable cause) {
        super(message, cause);
    }
}
