package com.craftpilot.subscriptionservice.controller.dto;

import com.craftpilot.subscriptionservice.model.subscription.entity.Subscription;
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
    private String status;
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
                .status(subscription.getStatus())
                .amount(subscription.getAmount())
                .description(subscription.getDescription())
                .paymentUrl(subscription.getPaymentUrl())
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .isActive(subscription.getIsActive())
                .isDeleted(subscription.getIsDeleted())
                .createdAt(subscription.getCreatedAt())
                .updatedAt(subscription.getUpdatedAt())
                .build();
    }

    public Subscription toEntity() {
        return Subscription.builder()
                .id(this.id)
                .userId(this.userId)
                .planId(this.planId)
                .status(this.status)
                .amount(this.amount)
                .description(this.description)
                .paymentUrl(this.paymentUrl)
                .startDate(this.startDate)
                .endDate(this.endDate)
                .isActive(this.isActive)
                .isDeleted(this.isDeleted)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }
} 