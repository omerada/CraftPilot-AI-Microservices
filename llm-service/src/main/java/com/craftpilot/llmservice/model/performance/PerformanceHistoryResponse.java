package com.craftpilot.llmservice.model.performance;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PerformanceHistoryResponse {
    private List<PerformanceHistoryEntry> history;

    @Data
    @Builder
    public static class PerformanceHistoryEntry {
        private String id;
        private String url;
        private long timestamp;
        private double performance;
    }
}
