package com.craftpilot.llmservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIRequest {
    private String requestId;
    private String userId;
    private String model;
    private String prompt;
    private List<Map<String, String>> messages;
    private Integer maxTokens;
    private Double temperature;
    private String requestType;
}