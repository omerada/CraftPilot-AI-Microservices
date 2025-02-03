package com.craftpilot.subscriptionservice.model.payment.iyzico;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IyzicoPaymentResult {
    private String status;
    private String paymentTransactionId;
    private String errorMessage;
} 