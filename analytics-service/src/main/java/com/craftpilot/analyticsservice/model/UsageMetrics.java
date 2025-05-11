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
@Document(collection = "usage_metrics")
public class UsageMetrics {
    @Id
    private String id;
    
    @Indexed
    private String userId;
    
    @Indexed
    private String serviceId;
    
    @Indexed
    private String modelId;
    
    @Indexed
    private ServiceType serviceType;
    
    private Map<String, Long> requestCounts;
    private Map<String, Double> tokenUsage;
    private Map<String, Long> errorCounts;
    private Map<String, Double> latencyMetrics;
    private Map<String, Object> customMetrics;
    
    @Indexed
    private LocalDateTime startTime;
    
    @Indexed
    private LocalDateTime endTime;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum ServiceType {
        LLM,
        CHAT,
        IMAGE,
        VIDEO,
    }
}