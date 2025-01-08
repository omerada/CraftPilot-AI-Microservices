package com.craftpilot.subscriptionservice.model.subscription.entity;

import com.craftpilot.subscriptionservice.model.auth.SubscriptionStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "subscription_plan", schema = "subscription_service")
@Getter
@Setter
public class SubscriptionPlanEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "price")
    private Double price;

    @Column(name = "duration_in_months")
    private Integer durationInMonths;  // Plan süresi (ay cinsinden)

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private SubscriptionStatus status;
}
