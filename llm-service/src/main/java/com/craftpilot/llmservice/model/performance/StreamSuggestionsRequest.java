package com.craftpilot.llmservice.model.performance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreamSuggestionsRequest {
    private String requestId;
    private String userId;
    private String model;
    private int maxTokens;
    private double temperature;
    private String language;
    private String url;
}
