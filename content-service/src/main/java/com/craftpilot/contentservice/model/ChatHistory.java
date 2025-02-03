package com.craftpilot.contentservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ChatHistory {
    private String id;
    private String userId;
    private String contentId;
    private List<ChatMessage> messages;
    private Map<String, String> metadata;
    private Instant createdAt;
    private Instant updatedAt;
} 