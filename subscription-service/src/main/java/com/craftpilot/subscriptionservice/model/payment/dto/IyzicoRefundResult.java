package com.craftpilot.subscriptionservice.model.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IyzicoRefundResult {
    private boolean success;
    private String transactionId;
    private String errorMessage;
    private String errorCode;
} 