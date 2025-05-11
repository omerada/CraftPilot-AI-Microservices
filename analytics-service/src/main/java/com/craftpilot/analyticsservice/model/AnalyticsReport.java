package com.craftpilot.analyticsservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
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
@Document(collection = "analytics_reports")
public class AnalyticsReport {
    @Id
    private String id;
    
    private String name;
    
    @Indexed
    private ReportType type;
    
    private String description;
    private Map<String, Object> parameters;
    private Map<String, Object> data;
    private List<String> tags;
    
    @Indexed
    private ReportStatus status;
    
    @Indexed
    private String createdBy;
    
    @Indexed
    private LocalDateTime reportStartTime;
    
    @Indexed
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