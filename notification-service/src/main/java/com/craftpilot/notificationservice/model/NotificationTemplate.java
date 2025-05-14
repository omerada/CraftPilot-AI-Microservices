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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "notification_templates")
public class NotificationTemplate {
    @Id
    private String id;

    @Indexed
    private String name;

    private String titleTemplate;
    private String contentTemplate;
    private Map<String, String> requiredVariables;
    private Map<String, Object> defaultValues;

    // Eski alanlar - geriye dönük uyumluluk için tutulabilir
    private String title;
    private String body;
    private String subject;
    private Map<String, Object> templateData;
    private NotificationType type;

    private boolean active;
    private boolean deleted;

    @Version
    private Long version;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}