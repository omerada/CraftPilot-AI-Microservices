package com.craftpilot.adminservice.model;

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
public class SystemAlert {
    @DocumentId
    private String id;
    
    private String serviceId;
    private AlertType alertType;
    private AlertSeverity severity;
    private String title;
    private String description;
    private Map<String, Object> alertData;
    private AlertStatus status;
    private List<String> affectedServices;
    private List<String> tags;
    private String assignedTo;
    private Map<String, Object> metadata;
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