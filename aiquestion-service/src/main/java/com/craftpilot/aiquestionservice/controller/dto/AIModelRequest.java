package com.craftpilot.aiquestionservice.controller.dto;

import com.craftpilot.aiquestionservice.model.enums.ModelType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
 

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIModelRequest {
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
} 