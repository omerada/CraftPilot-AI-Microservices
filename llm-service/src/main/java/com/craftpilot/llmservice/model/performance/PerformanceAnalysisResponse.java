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
    private String jobId; // Job ID'si ekle
    private double performance;
    private Map<String, Object> audits;
    private long timestamp;
    private String url;
    private Map<String, Object> categories;
    private String error;
    private String status; // İşlem durumu: PENDING, COMPLETED, ERROR
    private String message; // Kullanıcıya gösterilecek mesaj

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
