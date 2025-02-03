package com.craftpilot.subscriptionservice.model.subscription.entity;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.PropertyName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPlan {
    @DocumentId
    private String id;
    
    @PropertyName("name")
    private String name;
    
    @PropertyName("description")
    private String description;
    
    @PropertyName("price")
    private BigDecimal price;
    
    @PropertyName("currency")
    private String currency;
    
    @PropertyName("durationInDays")
    private Integer durationInDays;
    
    @PropertyName("requestLimit")
    private Integer requestLimit;
    
    @PropertyName("modelLimit")
    private Integer modelLimit;
    
    @PropertyName("hasAdvancedModels")
    private Boolean hasAdvancedModels;
    
    @PropertyName("hasPrioritySupport")
    private Boolean hasPrioritySupport;
    
    @PropertyName("hasTeamFeatures")
    private Boolean hasTeamFeatures;
    
    @PropertyName("hasCustomization")
    private Boolean hasCustomization;
    
    @PropertyName("maxTeamMembers")
    private Integer maxTeamMembers;
    
    @PropertyName("maxProjects")
    private Integer maxProjects;
    
    @PropertyName("isActive")
    private Boolean isActive;
    
    @PropertyName("isDeleted")
    private Boolean isDeleted;
    
    @PropertyName("features")
    private List<String> features;
    
    @PropertyName("createdAt")
    private LocalDateTime createdAt;
    
    @PropertyName("updatedAt")
    private LocalDateTime updatedAt;
}