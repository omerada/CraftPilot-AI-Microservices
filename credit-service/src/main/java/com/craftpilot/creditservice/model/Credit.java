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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "credits")
public class Credit {
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String userId;
    
    // Standard credits
    private BigDecimal balance;
    private BigDecimal totalCreditsEarned;
    private BigDecimal totalCreditsUsed;
    
    // Advanced credits
    private BigDecimal advancedBalance;
    private BigDecimal totalAdvancedCreditsEarned;
    private BigDecimal totalAdvancedCreditsUsed;
    
    // For backward compatibility
    private double lifetimeEarned;
    private double lifetimeSpent;
    
    private boolean deleted;
    
    private LocalDateTime lastUpdated;
    private LocalDateTime createdAt;
}