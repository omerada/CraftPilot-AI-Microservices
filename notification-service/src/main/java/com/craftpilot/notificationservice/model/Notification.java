package com.craftpilot.notificationservice.model;

import com.craftpilot.notificationservice.model.enums.NotificationType;
import com.craftpilot.notificationservice.model.enums.NotificationStatus;
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
    private String id;
    private String userId;
    private String templateId;
    private String recipient;
    private String recipientEmail;
    private String subject;
    private String title;
    private String content;
    private String body;
    private NotificationType type;
    private NotificationStatus status;
    private Map<String, Object> data;
    private LocalDateTime scheduledAt;
    private LocalDateTime scheduledTime;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime processedTime;
    private boolean sent;
    private boolean read;
    private boolean processed;
}