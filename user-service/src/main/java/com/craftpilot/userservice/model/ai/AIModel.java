package com.craftpilot.userservice.model.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "ai_models")
public class AIModel {
    @Id
    private String modelId;  
    private String modelName;
    private String provider;
    private Integer maxInputTokens;
    private String requiredPlan;
    private Integer creditCost;
    private String creditType;
    private String category;
    private Integer contextLength;
    private Double defaultTemperature;
    private String icon;
    private String description;
    private Double fee;
    private Boolean featured;
    private Integer maxTokens;
    private Boolean multimodal;
    private Boolean active;
}
