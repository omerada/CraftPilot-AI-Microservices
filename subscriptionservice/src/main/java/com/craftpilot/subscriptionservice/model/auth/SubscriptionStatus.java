package com.craftpilot.subscriptionservice.model.auth;

/**
 * Enum representing the status of a user's subscription.
 */
public enum SubscriptionStatus {

    ACTIVE("Active"),
    EXPIRED("Expired"),
    CANCELLED("Cancelled"),
    PENDING("Pending");

    private final String status;

    SubscriptionStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}

