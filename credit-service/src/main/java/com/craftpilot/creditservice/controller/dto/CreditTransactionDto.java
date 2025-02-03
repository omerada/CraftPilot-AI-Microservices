package com.craftpilot.creditservice.controller.dto;

import com.craftpilot.creditservice.model.CreditTransaction;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class CreditTransactionDto {
    private String id;
    private String userId;
    private String serviceId;
    private CreditTransaction.TransactionType type;
    private BigDecimal amount;
    private String description;
    private CreditTransaction.TransactionStatus status;
    private LocalDateTime createdAt;

    public static CreditTransactionDto fromEntity(CreditTransaction transaction) {
        return CreditTransactionDto.builder()
                .id(transaction.getId())
                .userId(transaction.getUserId())
                .serviceId(transaction.getServiceId())
                .type(transaction.getType())
                .amount(transaction.getAmount())
                .description(transaction.getDescription())
                .status(transaction.getStatus())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
} 