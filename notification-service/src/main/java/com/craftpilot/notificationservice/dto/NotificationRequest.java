package com.craftpilot.notificationservice.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class NotificationRequest {
    private String userId;
    private String templateId;
    private String type;
    private String title;
    private String body;
    private String token;
    private List<String> tokens;
    private String topic;
    private Map<String, Object> variables;
    private Map<String, Object> additionalData;
    private LocalDateTime scheduledAt;
}