package com.craftpilot.subscriptionservice.model.subscription.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO class for subscription request.
 */
@Getter
@Setter
public class SubscriptionRequest {

    private String userId;  // Kullanıcı ID'si
    private Long subscriptionPlanId;  // Abonelik planı ID'si
    private LocalDateTime startDate;  // Başlangıç tarihi
    private LocalDateTime endDate;  // Bitiş tarihi
}
