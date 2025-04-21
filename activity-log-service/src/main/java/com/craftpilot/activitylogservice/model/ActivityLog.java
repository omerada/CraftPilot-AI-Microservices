package com.craftpilot.activitylogservice.model;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.ServerTimestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLog {
    
    @DocumentId
    private String id;
    
    private String userId;
    private String actionType;
    
    @ServerTimestamp
    private Timestamp createdAt;
    
    private LocalDateTime timestamp;
    private Map<String, Object> metadata;
    
    public static ActivityLog fromEvent(ActivityEvent event) {
        return ActivityLog.builder()
                .id(UUID.randomUUID().toString())
                .userId(event.getUserId())
                .actionType(event.getActionType())
                .timestamp(event.getTimestamp())
                .metadata(event.getMetadata())
                .build();
    }
    
    public ZonedDateTime getZonedTimestamp() {
        return timestamp != null 
            ? timestamp.atZone(ZoneId.systemDefault()) 
            : null;
    }
}
