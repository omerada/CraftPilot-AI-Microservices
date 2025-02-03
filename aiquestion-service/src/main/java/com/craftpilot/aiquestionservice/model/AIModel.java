package com.craftpilot.aiquestionservice.model;

import com.craftpilot.aiquestionservice.model.enums.ModelType;
import com.google.cloud.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIModel {
    private String id;
    private String name;
    private String description;
    private ModelType type;
    private String provider;
    private String version;
    private String endpoint;
    private String apiKey;
    private Integer maxTokens;
    private Double temperature;
    private Boolean isActive;
    private Timestamp createdAt;
    private Timestamp updatedAt;
} 