package com.craftpilot.contentservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentEvent {
    private String eventId;
    private String contentId;
    private String userId;
    private ContentEventType eventType;
    private Content content;
    private Instant timestamp;
    private String correlationId;
}

enum ContentEventType {
    CREATED,
    UPDATED,
    DELETED,
    PUBLISHED,
    ARCHIVED
} 