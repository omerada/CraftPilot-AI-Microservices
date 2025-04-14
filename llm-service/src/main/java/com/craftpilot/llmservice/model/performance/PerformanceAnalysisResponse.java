package com.craftpilot.llmservice.model.performance;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
public class PerformanceAnalysisResponse {
    private double performance;
    private Map<String, AuditResult> audits;
    private long timestamp;
    private String url;
    private Map<String, CategoryResult> categories;
    private String id;

    @Data
    @Builder
    public static class AuditResult {
        private double score;
        private String displayValue;
        private String description;
    }

    @Data
    @Builder
    public static class CategoryResult {
        private double score;
    }
}
