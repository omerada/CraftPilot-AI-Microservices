package com.craftpilot.llmservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AIResponse {
    private String requestId;
    private String response;
    private String model;
    private Integer tokensUsed;
    private Long processingTime;
    private String status;
    private String error; 
    private Boolean success; 

    public static AIResponse error(String errorMessage) {
        return AIResponse.builder()
            .status("ERROR")
            .error(errorMessage)
            .success(false)
            .build();
    }

    public static AIResponse success(String response, String model, Integer tokensUsed, String requestId) {
        return AIResponse.builder()
            .status("SUCCESS")
            .response(response)
            .model(model)
            .tokensUsed(tokensUsed)
            .requestId(requestId)
            .success(true)
            .processingTime(System.currentTimeMillis())
            .build();
    }
}