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
@Document(collection = "audit_logs")
public class AuditLog {
    @Id
    private String id;
    
    @Indexed
    private String userId;
    
    @Indexed
    private String serviceId;
    
    @Indexed
    private LogType logType;
    
    private String action;
    
    @Indexed
    private String resource;
    
    private String resourceType;
    private Map<String, Object> requestData;
    private Map<String, Object> responseData;
    
    @Indexed
    private LogStatus status;
    
    private String ipAddress;
    private String userAgent;
    private Map<String, Object> metadata;
    
    @Indexed
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