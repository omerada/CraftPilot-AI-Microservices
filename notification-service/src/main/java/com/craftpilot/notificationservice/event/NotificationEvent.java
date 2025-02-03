package com.craftpilot.notificationservice.event;

import com.craftpilot.notificationservice.model.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {
    private String eventType;
    private String notificationId;
    private String userId;
    private Long timestamp;
    private Notification notification;
    
    public static NotificationEvent fromNotification(String eventType, Notification notification) {
        return NotificationEvent.builder()
                .eventType(eventType)
                .notificationId(notification.getId())
                .userId(notification.getUserId())
                .timestamp(System.currentTimeMillis())
                .notification(notification)
                .build();
    }
} 