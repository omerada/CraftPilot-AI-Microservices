package com.craftpilot.adminservice.model;

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
@Document(collection = "system_metrics")
public class SystemMetrics {
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String serviceId;
    
    @Indexed
    private ServiceType serviceType;
    
    @Indexed
    private ServiceStatus status;
    
    private Map<String, Double> resourceUsage; // CPU, Memory, Disk, Network
    private Map<String, Long> requestMetrics; // Total, Success, Failed
    private Map<String, Double> performanceMetrics; // Response Time, Throughput
    private Map<String, Object> customMetrics;
    
    @Indexed
    private LocalDateTime timestamp;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum ServiceType {
        API_GATEWAY,
        USER_SERVICE,
        LLM_SERVICE,
        CHAT_SERVICE,
        IMAGE_SERVICE, 
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