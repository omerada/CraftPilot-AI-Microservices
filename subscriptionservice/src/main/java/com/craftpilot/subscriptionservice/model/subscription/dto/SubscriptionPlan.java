package com.craftpilot.subscriptionservice.model.subscription.dto;

import com.craftpilot.subscriptionservice.model.auth.SubscriptionStatus;
import lombok.*;

/**
 * DTO class representing a subscription plan.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPlan {

    private Long id;  // Abonelik planı ID'si

    private String name;  // Abonelik planı adı

    private Double price;  // Abonelik planı ücreti

    private Integer durationInMonths;  // Abonelik süresi (ay olarak)

    private SubscriptionStatus status;  // Abonelik planı durumu
}

