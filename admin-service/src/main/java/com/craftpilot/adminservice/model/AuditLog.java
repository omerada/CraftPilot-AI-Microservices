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
public class AuditLog {
    @DocumentId
    private String id;
    
    private String userId;
    private String serviceId;
    private LogType logType;
    private String action;
    private String resource;
    private String resourceType;
    private Map<String, Object> requestData;
    private Map<String, Object> responseData;
    private LogStatus status;
    private String ipAddress;
    private String userAgent;
    private Map<String, Object> metadata;
    private LocalDateTime timestamp;
    private LocalDateTime createdAt;

    public enum LogType {
        SECURITY,
        OPERATION,
        DATA_ACCESS,
        CONFIGURATION,
        SYSTEM,
        USER_ACTION,
        API_ACCESS,
        ERROR,
        CUSTOM
    }

    public enum LogStatus {
        SUCCESS,
        FAILURE,
        WARNING,
        ERROR,
        INFO
    }
} 