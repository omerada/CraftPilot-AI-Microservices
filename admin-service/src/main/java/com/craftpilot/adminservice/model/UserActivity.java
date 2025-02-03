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
public class UserActivity {
    @DocumentId
    private String id;
    
    private String userId;
    private String serviceId;
    private ActivityType activityType;
    private Map<String, Object> activityData;
    private String ipAddress;
    private String userAgent;
    private ActivityStatus status;
    private Map<String, Object> metadata;
    private LocalDateTime timestamp;
    private LocalDateTime createdAt;

    public enum ActivityType {
        LOGIN,
        LOGOUT,
        SIGNUP,
        PASSWORD_CHANGE,
        PROFILE_UPDATE,
        SUBSCRIPTION_CHANGE,
        CREDIT_PURCHASE,
        SERVICE_ACCESS,
        API_CALL,
        SETTINGS_CHANGE,
        ADMIN_ACTION
    }

    public enum ActivityStatus {
        SUCCESS,
        FAILED,
        BLOCKED,
        SUSPICIOUS,
        PENDING
    }
} 