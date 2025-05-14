package com.craftpilot.notificationservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "notification_preferences")
public class NotificationPreference {
    @Id
    private String id;

    @Indexed(unique = true)
    private String userId;

    private Map<NotificationType, Set<String>> channelPreferences;
    private String email;
    private String deviceToken;
    private boolean emailVerified;
    private boolean phoneVerified;
    private Map<String, Object> additionalSettings;
    private boolean active;
    private boolean deleted;

    @Version
    private Long version;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Add explicit getter for Spring Data compatibility
    public boolean isDeleted() {
        return deleted;
    }
}