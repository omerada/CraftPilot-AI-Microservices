package com.craftpilot.analyticsservice.model;

import com.google.cloud.firestore.annotation.DocumentId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceMetrics {
    @DocumentId
    private String id;
    
    private String modelId;
    private String serviceId;
    private MetricType type;
    private Map<String, Double> metrics;
    private Map<String, Long> counts;
    private Map<String, Object> dimensions;
    private LocalDateTime timestamp;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum MetricType {
        LATENCY,
        THROUGHPUT,
        ERROR_RATE,
        SUCCESS_RATE,
        RESOURCE_USAGE,
        CUSTOM
    }
} 