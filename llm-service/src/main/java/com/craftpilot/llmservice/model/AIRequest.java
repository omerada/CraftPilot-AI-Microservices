package com.craftpilot.llmservice.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class AIRequest {
    private String model;
    private List<Map<String, String>> messages;
    private Integer maxTokens;
    private Double temperature;
    private String requestType;
} 