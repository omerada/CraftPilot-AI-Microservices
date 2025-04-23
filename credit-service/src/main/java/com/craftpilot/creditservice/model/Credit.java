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
public class Credit {
    private String id;
    private String userId;
    
    // Standart kredi bakiyesi
    private BigDecimal balance;
    private BigDecimal totalCreditsEarned;
    private BigDecimal totalCreditsUsed;
    
    // Gelişmiş kredi bakiyesi
    private BigDecimal advancedBalance;
    private BigDecimal totalAdvancedCreditsEarned;
    private BigDecimal totalAdvancedCreditsUsed;
    
    private LocalDateTime lastUpdated;
    private LocalDateTime createdAt;
    private boolean deleted;
}