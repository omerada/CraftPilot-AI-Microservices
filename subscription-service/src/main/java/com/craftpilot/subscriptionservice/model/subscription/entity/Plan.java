package com.craftpilot.subscriptionservice.model.subscription.entity;

import com.google.cloud.firestore.annotation.DocumentId;
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
public class Plan {
    @DocumentId
    private String id;
    
    private String name;
    private String description;
    private BigDecimal price;
    private String currency;
    private Integer durationInMonths;
    private boolean hasPrioritySupport;
    private boolean isActive;
    private Integer maxTeamMembers;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Builder.Default
    private boolean isDeleted = false;
} 