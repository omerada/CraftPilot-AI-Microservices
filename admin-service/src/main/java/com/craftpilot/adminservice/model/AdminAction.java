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
public class AdminAction {
    @DocumentId
    private String id;
    
    private String adminId;
    private ActionType actionType;
    private String targetId; // User ID, Service ID, etc.
    private String targetType; // User, Service, Setting, etc.
    private Map<String, Object> actionData;
    private ActionStatus status;
    private String reason;
    private Map<String, Object> metadata;
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