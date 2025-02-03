package com.craftpilot.subscriptionservice.model.subscription.entity;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.PropertyName;
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
public class Subscription {
    @DocumentId
    private String id;
    
    @PropertyName("userId")
    private String userId;
    
    @PropertyName("planId")
    private String planId;
    
    @PropertyName("status")
    private String status;
    
    @PropertyName("amount")
    private BigDecimal amount;
    
    @PropertyName("description")
    private String description;
    
    @PropertyName("paymentUrl")
    private String paymentUrl;
    
    @PropertyName("startDate")
    private LocalDateTime startDate;
    
    @PropertyName("endDate")
    private LocalDateTime endDate;
    
    @PropertyName("isActive")
    private Boolean isActive;
    
    @PropertyName("isDeleted")
    private Boolean isDeleted;
    
    @PropertyName("createdAt")
    private LocalDateTime createdAt;
    
    @PropertyName("updatedAt")
    private LocalDateTime updatedAt;
} 