package com.craftpilot.llmservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIResponse {
    private String requestId;
    private String userId;
    private String response;
    private String model;
    private Integer tokenCount;
    private Double responseTime;
    private Integer tokensUsed;
    
    // Eksik alanlar ekleniyor
    private boolean success;
    private String error;
}