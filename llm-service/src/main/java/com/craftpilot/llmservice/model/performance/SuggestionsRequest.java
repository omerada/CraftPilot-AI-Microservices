package com.craftpilot.llmservice.model.performance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuggestionsRequest {
    private Object analysisData;
    private String url;
    private String requestId;
    private String userId;
    private String language;
    
    // AI model configuration
    private String model;
    private Integer maxTokens;
    private Double temperature;
}
