package com.craftpilot.subscriptionservice.model.subscription.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "subscription_plans")
public class SubscriptionPlan {
    @Id
    private String id;

    @Indexed
    private String name;

    private String description;

    private BigDecimal price;

    private String currency;

    private Integer durationInDays;

    private Integer requestLimit;

    private Integer modelLimit;

    private Boolean hasAdvancedModels;

    private Boolean hasPrioritySupport;

    private Boolean hasTeamFeatures;

    private Boolean hasCustomization;

    private Integer maxTeamMembers;

    private Integer maxProjects;

    @Indexed
    private Boolean isActive;

    private Boolean isDeleted;

    private List<String> features;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}