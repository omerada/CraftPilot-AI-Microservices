package com.craftpilot.subscriptionservice.model.subscription.entity;

import com.craftpilot.subscriptionservice.model.subscription.enums.SubscriptionStatus;
import com.craftpilot.subscriptionservice.model.subscription.enums.SubscriptionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "subscriptions")
public class Subscription {
    @Id
    private String id;

    @Indexed(unique = true)
    private String userId;

    private String planId;
    private SubscriptionType type;
    private SubscriptionStatus status;
    private BigDecimal amount;
    private String description;
    private String paymentUrl;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private boolean autoRenewal;
    private boolean active; // Changed from isActive to active for standard naming
    private boolean deleted; // Changed from isDeleted to deleted for standard naming
    private int creditsPerMonth;
    private int availableCredits;
    private LocalDateTime lastCreditRefillDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}