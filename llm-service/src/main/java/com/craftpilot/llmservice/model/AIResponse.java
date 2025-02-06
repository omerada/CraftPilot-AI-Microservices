package com.craftpilot.llmservice.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AIResponse {
    private String id;
    private String model;
    private String content;
    private Long processingTime;
    private Integer tokenCount;
} 