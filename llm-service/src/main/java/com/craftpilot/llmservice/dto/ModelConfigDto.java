package com.craftpilot.llmservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelConfigDto {
    private String model;
    private Double temperature;
    private Integer maxTokens;
    private Map<String, Object> additionalParams;
}
