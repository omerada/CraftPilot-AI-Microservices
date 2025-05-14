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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "plans")
public class Plan {
    @Id
    private String id;

    @Indexed
    private String name;
    private String description;
    private BigDecimal price;
    private String currency;
    private Integer durationInMonths;
    private boolean hasPrioritySupport;

    @Indexed
    private boolean isActive;
    private Integer maxTeamMembers;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder.Default
    private boolean isDeleted = false;
}