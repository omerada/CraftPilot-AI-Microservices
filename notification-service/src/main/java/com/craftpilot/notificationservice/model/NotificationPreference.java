package com.craftpilot.notificationservice.model;

import com.google.cloud.firestore.annotation.DocumentId;
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
public class NotificationPreference {
    @DocumentId
    private String id;
    private String userId;
    private Map<NotificationType, Set<String>> channelPreferences;
    private String email;
    private String deviceToken;
    private boolean emailVerified;
    private boolean phoneVerified;
    private Map<String, Object> additionalSettings;
    private boolean active;
    private boolean deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
} 