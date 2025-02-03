package com.craftpilot.contentservice.event;

import com.craftpilot.contentservice.model.Content;
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
    private String eventType;
    private Content content;
    private String userId;
    private Instant timestamp;
} 