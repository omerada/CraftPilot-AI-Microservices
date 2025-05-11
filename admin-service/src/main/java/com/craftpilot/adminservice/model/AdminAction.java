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
@Document(collection = "admin_actions")
public class AdminAction {
    @Id
    private String id;
    
    @Indexed
    private String adminId;
    
    @Indexed
    private ActionType actionType;
    
    @Indexed
    private String targetId; // User ID, Service ID, etc.
    
    private String targetType; // User, Service, Setting, etc.
    private Map<String, Object> actionData;
    
    @Indexed
    private ActionStatus status;
    
    private String reason;
    private Map<String, Object> metadata;
    
    @Indexed
    private LocalDateTime timestamp;
    
    private LocalDateTime createdAt;

    public enum ActionType {
        USER_MANAGEMENT,
        SERVICE_MANAGEMENT,
        SYSTEM_CONFIGURATION,
        SECURITY_ACTION,
        BILLING_ACTION,
        CONTENT_MODERATION,
        ROLE_MANAGEMENT,
        SETTINGS_UPDATE,
        MAINTENANCE,
        MONITORING,
        CUSTOM
    }

    public enum ActionStatus {
        PENDING,
        APPROVED,
        REJECTED,
        COMPLETED,
        FAILED,
        CANCELLED
    }
}