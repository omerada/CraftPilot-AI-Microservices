package com.craftpilot.subscriptionservice.model.subscription.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSubscriptionRequest {
    @NotBlank(message = "User ID is required")
    private String userId;
    
    @NotBlank(message = "Plan ID is required")
    private String planId;
} 