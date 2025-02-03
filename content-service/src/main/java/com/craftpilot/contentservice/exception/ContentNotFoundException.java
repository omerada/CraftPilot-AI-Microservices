package com.craftpilot.contentservice.exception;

public class ContentNotFoundException extends ResourceNotFoundException {
    public ContentNotFoundException(String message) {
        super(message);
    }
} 