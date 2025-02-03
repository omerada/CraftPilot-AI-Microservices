package com.craftpilot.adminservice.model;

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
public class SystemMetrics {
    @DocumentId
    private String id;
    
    private String serviceId;
    private ServiceType serviceType;
    private ServiceStatus status;
    private Map<String, Double> resourceUsage; // CPU, Memory, Disk, Network
    private Map<String, Long> requestMetrics; // Total, Success, Failed
    private Map<String, Double> performanceMetrics; // Response Time, Throughput
    private Map<String, Object> customMetrics;
    private LocalDateTime timestamp;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum ServiceType {
        API_GATEWAY,
        USER_SERVICE,
        QUESTION_SERVICE,
        CHAT_SERVICE,
        IMAGE_SERVICE,
        CODE_SERVICE,
        TRANSLATION_SERVICE,
        CONTENT_SERVICE,
        MODEL_SERVICE,
        ANALYTICS_SERVICE,
        NOTIFICATION_SERVICE,
        CREDIT_SERVICE,
        SUBSCRIPTION_SERVICE
    }

    public enum ServiceStatus {
        HEALTHY,
        DEGRADED,
        DOWN,
        MAINTENANCE,
        UNKNOWN
    }
} 