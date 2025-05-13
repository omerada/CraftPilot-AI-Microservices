package com.craftpilot.activitylogservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "activity_logs")
public class ActivityLog {
    
    @Id
    private String id;
    
    @Indexed
    private String userId;
    
    @Indexed
    private String actionType;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @Indexed
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
