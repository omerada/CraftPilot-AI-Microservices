package com.craftpilot.activitylogservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "activity_logs")
public class ActivityLog {
    
    @Id
    private String id;
    
    private String userId;
    private String serviceName;
    private String actionType;
    private String description;
    private Map<String, Object> metadata;
    private LocalDateTime timestamp;
    private String ipAddress;
    private String userAgent;
}
