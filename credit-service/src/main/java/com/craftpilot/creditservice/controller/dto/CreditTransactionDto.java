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
    private BigDecimal amount;
    private String type;
    private String description;
    private String creditType;
    private LocalDateTime timestamp;

    public static CreditTransactionDto fromEntity(CreditTransaction transaction) {
        return CreditTransactionDto.builder()
                .id(transaction.getId())
                .userId(transaction.getUserId())
                .serviceId(transaction.getServiceId())
                .amount(transaction.getAmount())
                .type(transaction.getType())
                .description(transaction.getDescription())
                .creditType(transaction.getCreditType())
                .timestamp(transaction.getTimestamp())
                .build();
    }
}