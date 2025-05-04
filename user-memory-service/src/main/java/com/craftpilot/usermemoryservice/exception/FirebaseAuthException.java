package com.craftpilot.usermemoryservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class FirebaseAuthException extends RuntimeException {
    
    public FirebaseAuthException(String message) {
        super(message);
    }
    
    public FirebaseAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
