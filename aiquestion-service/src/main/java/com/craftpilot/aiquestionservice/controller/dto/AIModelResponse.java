package com.craftpilot.aiquestionservice.controller.dto;

import com.craftpilot.aiquestionservice.model.AIModel;
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
public class AIModelResponse {
    private String id;
    private String name;
    private String description;
    private ModelType type;
    private String provider;
    private String version;
    private String endpoint;
    private Integer maxTokens;
    private Double temperature;
    private Boolean isActive;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public static AIModelResponse fromModel(AIModel model) {
        return AIModelResponse.builder()
                .id(model.getId())
                .name(model.getName())
                .description(model.getDescription())
                .type(model.getType())
                .provider(model.getProvider())
                .version(model.getVersion())
                .endpoint(model.getEndpoint())
                .maxTokens(model.getMaxTokens())
                .temperature(model.getTemperature())
                .isActive(model.getIsActive())
                .createdAt(model.getCreatedAt())
                .updatedAt(model.getUpdatedAt())
                .build();
    }
} 