package com.craftpilot.notificationservice.dto;

import com.craftpilot.notificationservice.model.Notification;
import com.craftpilot.notificationservice.model.enums.NotificationStatus;
import com.craftpilot.notificationservice.model.enums.NotificationType;
import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationResponse {
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
    private boolean isRead;
    
    public static NotificationResponse fromEntity(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .templateId(notification.getTemplateId())
                .type(notification.getType())
                .title(notification.getTitle())
                .content(notification.getContent())
                .data(notification.getData())
                .status(notification.getStatus())
                .scheduledAt(notification.getScheduledAt())
                .sentAt(notification.getSentAt())
                .createdAt(notification.getCreatedAt())
                .updatedAt(notification.getUpdatedAt())
                .isRead(notification.isRead())
                .build();
    }
}