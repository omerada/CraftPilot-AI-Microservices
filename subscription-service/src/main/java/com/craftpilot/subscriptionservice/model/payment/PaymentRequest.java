package com.craftpilot.subscriptionservice.model.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {
    private String userId;
    private String planId;
    private BigDecimal amount;
    private String description;
    private String callbackUrl;
} 