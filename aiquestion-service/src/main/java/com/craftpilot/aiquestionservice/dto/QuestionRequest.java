package com.craftpilot.aiquestionservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionRequest {
    @NotBlank(message = "Soru metni boş olamaz")
    private String question;

    @Valid
    @NotNull(message = "Soru tercihleri boş olamaz")
    private QuestionPreferencesDto preferences;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionPreferencesDto {
        @NotBlank(message = "AI model seçimi zorunludur")
        private String aiModel;

        @NotBlank(message = "Dil seçimi zorunludur")
        private String language;

        private Integer maxTokens;
        private Double temperature;
        private Boolean useWebSearch;

        @NotBlank(message = "Yanıt stili seçimi zorunludur")
        private String responseStyle;

        @NotBlank(message = "Domain seçimi zorunludur")
        private String domain;

        private Map<String, Object> additionalParams;
    }
} 