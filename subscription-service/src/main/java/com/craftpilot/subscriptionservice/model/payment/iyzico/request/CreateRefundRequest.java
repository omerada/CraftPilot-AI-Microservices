package com.craftpilot.subscriptionservice.model.payment.iyzico.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRefundRequest {
    private String paymentTransactionId;
    private BigDecimal amount;
    private String currency;
    private String reason;
} 