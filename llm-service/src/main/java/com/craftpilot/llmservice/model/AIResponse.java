package com.craftpilot.llmservice.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
@JsonInclude(JsonInclude.Include.NON_NULL) // Null değerleri JSON çıktısından çıkarır
public class AIResponse {
    private String response;
    private String model;
    private Integer tokensUsed;
    private Boolean success; 

    public static AIResponse error(String errorMessage) {
        return AIResponse.builder()
            .response(errorMessage)
            .success(false)
            .build();
    }

    /**
     * Başarılı bir yanıt oluşturmak için builder metodunu kullandıran yardımcı statik metot
     */
    public static AIResponse success(String response, String model, Integer tokenCount, String requestId) {
        return AIResponse.builder()
            .response(response)
            .model(model)
            .tokenCount(tokenCount)
            .requestId(requestId)
            .success(true)
            .build();
    }
}