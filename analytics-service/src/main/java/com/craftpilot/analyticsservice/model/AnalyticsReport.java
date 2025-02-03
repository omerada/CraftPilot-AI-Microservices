package com.craftpilot.analyticsservice.model;

import com.google.cloud.firestore.annotation.DocumentId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsReport {
    @DocumentId
    private String id;
    
    private String name;
    private ReportType type;
    private String description;
    private Map<String, Object> parameters;
    private Map<String, Object> data;
    private List<String> tags;
    private ReportStatus status;
    private String createdBy;
    private LocalDateTime reportStartTime;
    private LocalDateTime reportEndTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum ReportType {
        USAGE_SUMMARY,
        PERFORMANCE_ANALYSIS,
        ERROR_ANALYSIS,
        COST_ANALYSIS,
        USER_BEHAVIOR,
        MODEL_COMPARISON,
        CUSTOM
    }

    public enum ReportStatus {
        SCHEDULED,
        GENERATING,
        COMPLETED,
        FAILED,
        ARCHIVED
    }
} 