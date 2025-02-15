package com.craftpilot.notificationservice.event;

import com.craftpilot.notificationservice.model.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {
    private String eventId;
    private String userId;
    private String eventType;
    private String notificationId;
    private LocalDateTime timestamp;
    private Notification notification;
    private String error;

    public static NotificationEvent fromNotification(String eventType, Notification notification) {
        return NotificationEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .userId(notification.getUserId())
                .eventType(eventType)
                .notificationId(notification.getId())
                .timestamp(LocalDateTime.now())
                .notification(notification)
                .build();
    }

    public static NotificationEvent error(Notification notification, String error) {
        return NotificationEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .userId(notification.getUserId())
                .eventType("NOTIFICATION_ERROR")
                .notificationId(notification.getId())
                .timestamp(LocalDateTime.now())
                .notification(notification)
                .error(error)
                .build();
    }
}