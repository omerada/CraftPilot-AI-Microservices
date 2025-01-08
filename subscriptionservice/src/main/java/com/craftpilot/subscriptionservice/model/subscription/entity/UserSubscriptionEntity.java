package com.craftpilot.subscriptionservice.model.subscription.entity;

import com.craftpilot.subscriptionservice.model.auth.SubscriptionStatus;
import com.craftpilot.subscriptionservice.model.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_subscription", schema = "subscription_service")
@Getter
@Setter
public class UserSubscriptionEntity extends BaseEntity {  // BaseEntity'i extend ettik

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;  // Kullanıcı ID'si (UUID veya String)

    @Column(name = "subscription_plan_id", nullable = false)
    private Long subscriptionPlanId;  // Abonelik planı ID'si

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SubscriptionStatus status;  // Abonelik durumu
}
