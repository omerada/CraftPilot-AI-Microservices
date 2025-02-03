package com.craftpilot.notificationservice.exception;

public class ResourceNotFoundException extends NotificationException {
    
    public ResourceNotFoundException(String resourceType, String resourceId) {
        super(String.format("%s not found with id: %s", resourceType, resourceId));
    }
    
    public ResourceNotFoundException(String resourceType, String field, String value) {
        super(String.format("%s not found with %s: %s", resourceType, field, value));
    }
} 