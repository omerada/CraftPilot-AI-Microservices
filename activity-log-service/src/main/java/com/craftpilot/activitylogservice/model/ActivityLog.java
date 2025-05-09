package com.craftpilot.activitylogservice.model;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.Exclude;
import com.google.cloud.firestore.annotation.ServerTimestamp;
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
public class ActivityLog {
    
    @DocumentId
    private String id;
    
    private String userId;
    private String actionType;
    
    @ServerTimestamp
    private Timestamp createdAt;
    
    // Change this field from LocalDateTime to a Firestore-compatible Date
    private Date eventTime;
    
    // Keep the original LocalDateTime as a transient field
    @Exclude
    private transient LocalDateTime timestamp;
    
    private Map<String, Object> metadata;
    
    public static ActivityLog fromEvent(ActivityEvent event) {
        ActivityLog log = ActivityLog.builder()
                .id(UUID.randomUUID().toString())
                .userId(event.getUserId())
                .actionType(event.getActionType())
                .metadata(event.getMetadata())
                .build();
                
        // Convert LocalDateTime to Date for Firestore
        if (event.getTimestamp() != null) {
            log.setEventTime(java.util.Date.from(
                event.getTimestamp().atZone(ZoneId.systemDefault()).toInstant()));
            log.setTimestamp(event.getTimestamp());
        }
        
        return log;
    }
    
    @Exclude
    public ZonedDateTime getZonedTimestamp() {
        if (timestamp != null) {
            return timestamp.atZone(ZoneId.systemDefault());
        } else if (eventTime != null) {
            // Convert back from Date to ZonedDateTime if needed
            return eventTime.toInstant().atZone(ZoneId.systemDefault());
        }
        return null;
    }
    
    @Exclude
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
