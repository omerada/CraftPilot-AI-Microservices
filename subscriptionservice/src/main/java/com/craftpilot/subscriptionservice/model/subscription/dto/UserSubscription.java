package com.craftpilot.subscriptionservice.model.subscription.dto;

import com.craftpilot.subscriptionservice.model.auth.SubscriptionStatus;
import com.craftpilot.subscriptionservice.model.common.BaseDomainModel;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO class representing a user subscription.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSubscription extends BaseDomainModel {

    private Long id;  // Abonelik ID'si

    private String userId;  // Kullanıcı ID'si (UUID veya String)

    private Long subscriptionPlanId;  // Abonelik Planı ID'si

    private LocalDateTime startDate;  // Başlangıç tarihi

    private LocalDateTime endDate;  // Bitiş tarihi

    private SubscriptionStatus status;  // Abonelik durumu
}
