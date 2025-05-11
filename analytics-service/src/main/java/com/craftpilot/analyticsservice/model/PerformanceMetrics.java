package com.craftpilot.analyticsservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
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
    
    private Double responseTime;
    private Long tokensPerSecond;
    private Double accuracy;
    private Double relevanceScore;
    private Map<String, Double> customScores;
    private Map<String, Object> metadata;
    
    @Indexed
    private LocalDateTime timestamp;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum MetricType {
        RESPONSE_TIME,
        THROUGHPUT,
        ACCURACY,
        RELEVANCE,
        CUSTOM
    }
}