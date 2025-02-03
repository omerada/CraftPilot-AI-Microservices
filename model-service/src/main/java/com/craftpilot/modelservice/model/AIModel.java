package com.craftpilot.modelservice.model;

import com.google.cloud.firestore.annotation.DocumentId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIModel {
    @DocumentId
    private String id;
    
    private String name;
    private String provider; // OpenAI, Anthropic, vb.
    private String version;
    private String description;
    private ModelType type;
    private ModelStatus status;
    private Map<String, Object> configuration;
    private Map<String, Object> metrics;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum ModelType {
        TEXT,
        IMAGE,
        CODE,
        CHAT,
        TRANSLATION,
        AUDIO,
        VIDEO
    }

    public enum ModelStatus {
        ACTIVE,
        INACTIVE,
        DEPRECATED,
        TESTING,
        FAILED
    }
} 