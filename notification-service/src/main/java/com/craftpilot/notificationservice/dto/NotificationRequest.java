package com.craftpilot.notificationservice.dto;

import com.craftpilot.notificationservice.model.Notification.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {
    
    @NotBlank(message = "User ID is required")
    private String userId;
    
    @NotBlank(message = "Template ID is required")
    private String templateId;
    
    @NotNull(message = "Notification type is required")
    private NotificationType type;
    
    private Map<String, Object> variables;
    
    private LocalDateTime scheduledAt;
    
    private Map<String, Object> additionalData;

    private String token;
    private List<String> tokens;
    private String topic;
    private String title;
    private String body;
} 