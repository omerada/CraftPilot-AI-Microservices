package com.craftpilot.notificationservice.model;

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
public class NotificationTemplate {
    private String id;
    private String name;
    private String description;
    private String category;
    private NotificationType type;
    private String titleTemplate;
    private String contentTemplate;
    private Map<String, String> requiredVariables;
    private Map<String, Object> defaultValues;
    private boolean active;
    private boolean deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}