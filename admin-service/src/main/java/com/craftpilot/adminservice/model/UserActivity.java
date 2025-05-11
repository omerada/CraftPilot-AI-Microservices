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
@Document(collection = "user_activities")
public class UserActivity {
    @Id
    private String id;
    
    @Indexed
    private String userId;
    
    @Indexed
    private String serviceId;
    
    @Indexed
    private ActivityType activityType;
    
    private Map<String, Object> activityData;
    private String ipAddress;
    private String userAgent;
    
    @Indexed
    private ActivityStatus status;
    
    private Map<String, Object> metadata;
    
    @Indexed
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