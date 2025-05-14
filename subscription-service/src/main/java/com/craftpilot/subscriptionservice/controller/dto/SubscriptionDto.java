package com.craftpilot.subscriptionservice.controller.dto;

import com.craftpilot.subscriptionservice.model.subscription.entity.Subscription;
import com.craftpilot.subscriptionservice.model.subscription.enums.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionDto {
    private String id;
    private String userId;
    private String planId;
    private String status; // Keep as String in DTO
    private BigDecimal amount;
    private String description;
    private String paymentUrl;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean isActive;
    private Boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SubscriptionDto fromEntity(Subscription subscription) {
        return SubscriptionDto.builder()
                .id(subscription.getId())
                .userId(subscription.getUserId())
                .planId(subscription.getPlanId())
                .status(subscription.getStatus().name()) // Convert enum to String
                .amount(subscription.getAmount())
                .description(subscription.getDescription())
                .paymentUrl(subscription.getPaymentUrl())
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .isActive(subscription.isActive()) // Use correct getter method
                .isDeleted(subscription.isDeleted()) // Use correct getter method
                .createdAt(subscription.getCreatedAt())
                .updatedAt(subscription.getUpdatedAt())
                .build();
    }

    public Subscription toEntity() {
        return Subscription.builder()
                .id(this.id)
                .userId(this.userId)
                .planId(this.planId)
                .status(this.status != null ? SubscriptionStatus.valueOf(this.status) : null) // Convert String to enum
                .amount(this.amount)
                .description(this.description)
                .paymentUrl(this.paymentUrl)
                .startDate(this.startDate)
                .endDate(this.endDate)
                .active(this.isActive) // Use the correct field name
                .deleted(this.isDeleted) // Use the correct field name
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }
}