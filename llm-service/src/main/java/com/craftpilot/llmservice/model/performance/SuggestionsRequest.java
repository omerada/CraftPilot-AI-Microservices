package com.craftpilot.llmservice.model.performance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuggestionsRequest {
    private String userId;
    private String model;
    private Integer maxTokens;
    private Double temperature;
    private String language;
    private String analysisData;
    private String url;
    private String requestId;
}
