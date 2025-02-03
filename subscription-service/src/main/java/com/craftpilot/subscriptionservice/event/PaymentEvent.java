package com.craftpilot.subscriptionservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {
    private String eventType;
    private String paymentId;
    private String subscriptionId;
    private String userId;
    private BigDecimal amount;
    private String status;
    private Long timestamp;
} 