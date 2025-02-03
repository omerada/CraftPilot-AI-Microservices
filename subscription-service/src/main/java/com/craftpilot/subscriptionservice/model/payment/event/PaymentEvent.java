package com.craftpilot.subscriptionservice.model.payment.event;

import com.craftpilot.subscriptionservice.model.payment.enums.PaymentMethod;
import com.craftpilot.subscriptionservice.model.payment.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {

    private String id;
    private String userId;
    private String subscriptionId;
    private Double amount;
    private String currency;
    private PaymentStatus status;
    private PaymentMethod paymentMethod;
    private String transactionId;
    private String errorMessage;
    private String eventType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
} 