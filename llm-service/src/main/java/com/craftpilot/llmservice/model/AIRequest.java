package com.craftpilot.llmservice.model;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AIRequest {
    private String requestId;
    private String userId;
    private String model;
    private Integer maxTokens;
    private Double temperature;
    private String language;
    private Boolean stream;
    private String requestType;
    private String prompt;
    private String systemPrompt;
    private List<Map<String, Object>> messages;
}