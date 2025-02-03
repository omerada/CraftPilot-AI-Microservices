package com.craftpilot.aiquestionservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class ModelNotFoundException extends ResponseStatusException {
    
    public ModelNotFoundException(String modelId) {
        super(HttpStatus.NOT_FOUND, "AI Model not found with id: " + modelId);
    }
    
    public ModelNotFoundException(String message, String modelId) {
        super(HttpStatus.NOT_FOUND, message + ": " + modelId);
    }
} 