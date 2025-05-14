package com.craftpilot.notificationservice.model;

import com.craftpilot.notificationservice.model.enums.NotificationType;
import com.craftpilot.notificationservice.model.enums.NotificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
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
public class Notification {
    @Id
    private String id;

    @Indexed
    private String userId;

    private String templateId;
    private NotificationType type;
    private String title;
    private String content;
    private String recipient;
    private String recipientEmail;
    private String subject;
    private Map<String, Object> data;
    private NotificationStatus status;

    private LocalDateTime scheduledAt;
    private LocalDateTime scheduledTime; // Alternatif alan - geriye dönük uyumluluk
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private boolean read;
    private boolean processed;
    private LocalDateTime processedTime;

    // Yardımcı metotlar - Instant - LocalDateTime dönüşümleri için
    public Instant getScheduledAtAsInstant() {
        return scheduledAt != null ? scheduledAt.atZone(java.time.ZoneId.systemDefault()).toInstant() : null;
    }

    public void setScheduledAtFromInstant(Instant instant) {
        this.scheduledAt = instant != null ? LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault()) : null;
    }
}