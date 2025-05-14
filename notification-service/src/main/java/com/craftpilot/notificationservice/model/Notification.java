package com.craftpilot.notificationservice.model;

import com.craftpilot.notificationservice.model.enums.NotificationType;
import com.craftpilot.notificationservice.model.enums.NotificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "notifications")
@CompoundIndexes({
        @CompoundIndex(name = "user_read_deleted_idx", def = "{'userId': 1, 'read': 1, 'deleted': 1}"),
        @CompoundIndex(name = "scheduled_deleted_idx", def = "{'scheduledAt': 1, 'deleted': 1}")
})
public class Notification {
    @Id
    private String id;

    @Indexed
    private String userId;

    private String title;
    private String body;
    private String imageUrl;
    private String actionUrl;
    private Map<String, Object> data;
    private NotificationType type;
    private NotificationChannel channel;
    private NotificationStatus status;
    private boolean read;
    private boolean deleted;

    @Indexed
    private Instant scheduledAt;

    private Instant sentAt;
    private Instant readAt;

    @Version
    private Long version;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}