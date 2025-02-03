package com.craftpilot.subscriptionservice.exception;

public class ActiveSubscriptionExistsException extends RuntimeException {
    public ActiveSubscriptionExistsException(String message) {
        super(message);
    }
} 