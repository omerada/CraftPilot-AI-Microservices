package com.craftpilot.notificationservice.model;

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
public class Notification {
    @DocumentId
    private String id;
    private String userId;
    private String templateId;
    private NotificationType type;
    private String title;
    private String content;
    private Map<String, Object> data;
    private NotificationStatus status;
    private LocalDateTime scheduledAt;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean read;
    private boolean deleted;

    public enum NotificationType {
        EMAIL,
        PUSH,
        IN_APP,
        SMS
    }

    public enum NotificationStatus {
        PENDING,
        SCHEDULED,
        SENT,
        FAILED
    }

    public enum NotificationPriority {
        LOW,
        MEDIUM,
        HIGH,
        URGENT
    }
} 