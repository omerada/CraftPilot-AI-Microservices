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
public class UsageMetrics {
    @DocumentId
    private String id;
    
    private String userId;
    private String serviceId;
    private String modelId;
    private ServiceType serviceType;
    private Map<String, Long> requestCounts;
    private Map<String, Double> tokenUsage;
    private Map<String, Long> errorCounts;
    private Map<String, Double> latencyMetrics;
    private Map<String, Object> customMetrics;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum ServiceType {
        QUESTION,
        CHAT,
        IMAGE,
        CODE,
        TRANSLATION,
        CONTENT,
        MODEL
    }
} 