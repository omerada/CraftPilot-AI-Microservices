package com.craftpilot.subscriptionservice.controller.dto;

import com.craftpilot.subscriptionservice.model.subscription.entity.SubscriptionPlan;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubscriptionPlanDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    private BigDecimal price;

    @NotBlank(message = "Currency is required")
    private String currency;

    @NotNull(message = "Duration in days is required")
    @Positive(message = "Duration must be positive")
    private Integer durationInDays;

    @NotNull(message = "Request limit is required")
    @Positive(message = "Request limit must be positive")
    private Integer requestLimit;

    @NotNull(message = "Model limit is required")
    @Positive(message = "Model limit must be positive")
    private Integer modelLimit;

    private Boolean hasAdvancedModels;
    private Boolean hasPrioritySupport;
    private Boolean hasTeamFeatures;
    private Boolean hasCustomization;

    @NotNull(message = "Max team members is required")
    @Positive(message = "Max team members must be positive")
    private Integer maxTeamMembers;

    @NotNull(message = "Max projects is required")
    @Positive(message = "Max projects must be positive")
    private Integer maxProjects;

    private Boolean isActive;
    private Boolean isDeleted;
    private List<String> features;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SubscriptionPlanDto fromEntity(SubscriptionPlan plan) {
        return SubscriptionPlanDto.builder()
                .id(plan.getId())
                .name(plan.getName())
                .description(plan.getDescription())
                .price(plan.getPrice())
                .currency(plan.getCurrency())
                .durationInDays(plan.getDurationInDays())
                .requestLimit(plan.getRequestLimit())
                .modelLimit(plan.getModelLimit())
                .hasAdvancedModels(plan.getHasAdvancedModels())
                .hasPrioritySupport(plan.getHasPrioritySupport())
                .hasTeamFeatures(plan.getHasTeamFeatures())
                .hasCustomization(plan.getHasCustomization())
                .maxTeamMembers(plan.getMaxTeamMembers())
                .maxProjects(plan.getMaxProjects())
                .isActive(plan.getIsActive())
                .isDeleted(plan.getIsDeleted())
                .features(plan.getFeatures())
                .createdAt(plan.getCreatedAt())
                .updatedAt(plan.getUpdatedAt())
                .build();
    }

    public SubscriptionPlan toEntity() {
        return SubscriptionPlan.builder()
                .id(this.id)
                .name(this.name)
                .description(this.description)
                .price(this.price)
                .currency(this.currency)
                .durationInDays(this.durationInDays)
                .requestLimit(this.requestLimit)
                .modelLimit(this.modelLimit)
                .hasAdvancedModels(this.hasAdvancedModels)
                .hasPrioritySupport(this.hasPrioritySupport)
                .hasTeamFeatures(this.hasTeamFeatures)
                .hasCustomization(this.hasCustomization)
                .maxTeamMembers(this.maxTeamMembers)
                .maxProjects(this.maxProjects)
                .isActive(this.isActive)
                .isDeleted(this.isDeleted)
                .features(this.features)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }
}