package com.craftpilot.adminservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "system_alerts")
public class SystemAlert {
    @Id
    private String id;
    
    @Indexed
    private String serviceId;
    
    @Indexed
    private AlertType alertType;
    
    @Indexed
    private AlertSeverity severity;
    
    private String title;
    private String description;
    private Map<String, Object> alertData;
    
    @Indexed
    private AlertStatus status;
    
    private List<String> affectedServices;
    private List<String> tags;
    
    @Indexed
    private String assignedTo;
    
    private Map<String, Object> metadata;
    
    @Indexed
    private LocalDateTime timestamp;
    
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum AlertType {
        SYSTEM_HEALTH,
        PERFORMANCE,
        SECURITY,
        ERROR,
        THRESHOLD,
        RESOURCE_USAGE,
        SERVICE_STATUS,
        MAINTENANCE,
        CUSTOM
    }

    public enum AlertSeverity {
        CRITICAL,
        HIGH,
        MEDIUM,
        LOW,
        INFO
    }

    public enum AlertStatus {
        ACTIVE,
        ACKNOWLEDGED,
        IN_PROGRESS,
        RESOLVED,
        CLOSED,
        FALSE_POSITIVE
    }
}