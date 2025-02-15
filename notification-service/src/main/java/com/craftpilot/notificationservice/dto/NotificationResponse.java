package com.craftpilot.notificationservice.dto;

import com.craftpilot.notificationservice.model.Notification;
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
public class NotificationResponse {
    private String id;
    private String userId;
    private String templateId;
    private String type;
    private String title;
    private String content;
    private Map<String, Object> data;
    private String status;
    private LocalDateTime scheduledAt;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean read;

    public static NotificationResponse fromEntity(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .templateId(notification.getTemplateId())
                .type(notification.getType().name())
                .title(notification.getTitle())
                .content(notification.getContent())
                .data(notification.getData())
                .status(notification.getStatus().name())
                .scheduledAt(notification.getScheduledAt())
                .sentAt(notification.getSentAt())
                .createdAt(notification.getCreatedAt())
                .updatedAt(notification.getUpdatedAt())
                .read(notification.isRead())
                .build();
    }
}