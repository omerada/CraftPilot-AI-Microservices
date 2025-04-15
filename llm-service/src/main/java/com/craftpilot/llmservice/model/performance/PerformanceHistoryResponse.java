package com.craftpilot.llmservice.model.performance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceHistoryResponse {
    private String url;
    private String error;
    private List<PerformanceHistoryEntry> history;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceHistoryEntry {
        private String id;
        private String url;
        private LocalDateTime timestamp;
        private Double performance;
    }
}
