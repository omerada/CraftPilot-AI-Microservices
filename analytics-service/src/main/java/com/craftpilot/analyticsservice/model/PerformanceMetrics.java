package com.craftpilot.analyticsservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "performance_metrics")
public class PerformanceMetrics {
    @Id
    private String id;
    
    @Indexed
    private String modelId;
    
    @Indexed
    private String serviceId;
    
    @Indexed
    private MetricType type;
    
    private Map<String, Double> metrics;
    private Map<String, Long> counts;
    private Map<String, Object> dimensions;
    
    @Indexed
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