package com.craftpilot.notificationservice.dto;

import com.craftpilot.notificationservice.model.NotificationPreference;
import com.craftpilot.notificationservice.model.NotificationType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationPreferenceResponse {
    private String id;
    private String userId;
    private Map<NotificationType, Set<String>> channelPreferences;
    private String email;
    private String deviceToken;
    private boolean emailVerified;
    private boolean phoneVerified;
    private Map<String, Object> additionalSettings;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static NotificationPreferenceResponse fromEntity(NotificationPreference preference) {
        return NotificationPreferenceResponse.builder()
                .id(preference.getId())
                .userId(preference.getUserId())
                .channelPreferences(preference.getChannelPreferences())
                .email(preference.getEmail())
                .deviceToken(preference.getDeviceToken())
                .emailVerified(preference.isEmailVerified())
                .phoneVerified(preference.isPhoneVerified())
                .additionalSettings(preference.getAdditionalSettings())
                .active(preference.isActive())
                .createdAt(preference.getCreatedAt())
                .updatedAt(preference.getUpdatedAt())
                .build();
    }
} 