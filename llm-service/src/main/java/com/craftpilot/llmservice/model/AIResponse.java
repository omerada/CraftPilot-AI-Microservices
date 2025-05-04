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
    private String response;
    private String model;
    private String requestId;
    private boolean success;
    private String error;
    private long tokenCount;
    private double processingTime;
    
    // Eksik metodlar ekleniyor
    public AIResponse error(String errorMessage) {
        this.error = errorMessage;
        this.success = false;
        return this;
    }
    
    public long getTokensUsed() {
        return this.tokenCount;
    }
}