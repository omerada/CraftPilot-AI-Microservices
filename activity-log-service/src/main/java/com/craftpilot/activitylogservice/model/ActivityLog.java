package com.craftpilot.activitylogservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
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
    
    private LocalDateTime createdAt;
    
    @Indexed
    private Date eventTime;
    
    @Transient
    private transient LocalDateTime timestamp;
    
    private Map<String, Object> metadata;
    
    public static ActivityLog fromEvent(ActivityEvent event) {
        ActivityLog log = ActivityLog.builder()
                .id(UUID.randomUUID().toString())
                .userId(event.getUserId())
                .actionType(event.getActionType())
                .metadata(event.getMetadata())
                .createdAt(LocalDateTime.now())
                .build();
                
        // Convert LocalDateTime to Date for MongoDB
        if (event.getTimestamp() != null) {
            log.setEventTime(java.util.Date.from(
                event.getTimestamp().atZone(ZoneId.systemDefault()).toInstant()));
            log.setTimestamp(event.getTimestamp());
        }
        
        return log;
    }
    
    @Transient
    public ZonedDateTime getZonedTimestamp() {
        if (timestamp != null) {
            return timestamp.atZone(ZoneId.systemDefault());
        } else if (eventTime != null) {
            // Convert back from Date to ZonedDateTime if needed
            return eventTime.toInstant().atZone(ZoneId.systemDefault());
        }
        return null;
    }
    
    @Transient
    public LocalDateTime getTimestamp() {
        if (timestamp != null) {
            return timestamp;
        } else if (eventTime != null) {
            // Convert from Date to LocalDateTime
            return eventTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        }
        return null;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
        if (timestamp != null) {
            this.eventTime = java.util.Date.from(
                timestamp.atZone(ZoneId.systemDefault()).toInstant());
        }
    }
}
