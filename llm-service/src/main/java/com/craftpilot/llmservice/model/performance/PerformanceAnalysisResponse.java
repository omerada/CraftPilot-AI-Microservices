package com.craftpilot.llmservice.model.performance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceAnalysisResponse {
    private String id;
    private double performance;
    private Map<String, AuditResult> audits;
    private long timestamp;
    private String url;
    private Map<String, CategoryResult> categories;
    private String error; // Hata mesajı için yeni alan

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuditResult {
        private double score;
        private String displayValue;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryResult {
        private double score;
    }
}
