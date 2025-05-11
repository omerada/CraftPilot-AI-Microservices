package com.craftpilot.creditservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "credit_transactions")
public class CreditTransaction {
    @Id
    private String id;
    
    @Indexed
    private String userId;
    
    private String serviceId;
    
    private BigDecimal amount;
    private String description;
    private String type; // String representation (CREDIT/DEBIT)
    private TransactionType type2; // Enum representation
    private TransactionStatus status;
    private String creditType; // String representation
    private CreditType creditTypeEnum; // Enum representation
    private String relatedTransactionId;
    private Map<String, Object> metadata;
    
    private boolean deleted;
    
    @Indexed
    private LocalDateTime timestamp;
    private Date updatedAt;
    
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