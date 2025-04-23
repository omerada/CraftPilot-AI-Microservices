package com.craftpilot.creditservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditTransaction {
    private String id;
    private String userId;
    private String serviceId;
    private BigDecimal amount;
    private String type; // CREDIT veya DEBIT
    private TransactionType type2; // Enum representation of type
    private String description;
    private String creditType; // STANDARD veya ADVANCED
    private LocalDateTime timestamp;
    private LocalDateTime createdAt;
    private TransactionStatus status;
    private boolean deleted;
    
    public enum TransactionType {
        CREDIT,
        DEBIT
    }
    
    public enum TransactionStatus {
        PENDING,
        COMPLETED,
        FAILED,
        CANCELLED
    }
}