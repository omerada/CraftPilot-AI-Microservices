package com.craftpilot.subscriptionservice.model.payment.iyzico;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IyzicoRefundResult {
    private String status;
    private String refundTransactionId;
    private String errorMessage;
} 