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
    private BigDecimal balance;
    private BigDecimal totalCreditsEarned;
    private BigDecimal totalCreditsUsed;
    private LocalDateTime lastUpdated;
    private LocalDateTime createdAt;
    private boolean deleted;
} 