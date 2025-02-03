package com.craftpilot.notificationservice.dto;

import com.craftpilot.notificationservice.model.NotificationType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;
import java.util.Set;

@Data
public class NotificationPreferenceRequest {
    @NotBlank(message = "User ID is required")
    private String userId;

    @NotNull(message = "Channel preferences are required")
    private Map<NotificationType, Set<String>> channelPreferences;

    @Email(message = "Invalid email format")
    private String email;

    private String deviceToken;
    private Map<String, Object> additionalSettings;
} 