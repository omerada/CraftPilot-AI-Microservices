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
    private String url;
    private String requestId;
    private String userId;
    private String language;
    private String model;
    private Integer maxTokens;
    private Double temperature;
}
