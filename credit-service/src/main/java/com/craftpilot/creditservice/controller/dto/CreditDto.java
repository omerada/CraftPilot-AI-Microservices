package com.craftpilot.creditservice.controller.dto;

import com.craftpilot.creditservice.model.Credit;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class CreditDto {
    private String id;
    private String userId;
    private BigDecimal balance;
    private BigDecimal totalCreditsEarned;
    private BigDecimal totalCreditsUsed;
    private LocalDateTime lastUpdated;
    private LocalDateTime createdAt;

    public static CreditDto fromEntity(Credit credit) {
        return CreditDto.builder()
                .id(credit.getId())
                .userId(credit.getUserId())
                .balance(credit.getBalance())
                .totalCreditsEarned(credit.getTotalCreditsEarned())
                .totalCreditsUsed(credit.getTotalCreditsUsed())
                .lastUpdated(credit.getLastUpdated())
                .createdAt(credit.getCreatedAt())
                .build();
    }
} 