package com.craftpilot.userservice.model.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "ai_models")
public class AIModel {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String modelId;
    
    private String modelName;
    private String provider;
    private String providerId;
    private Integer maxInputTokens;
    private String requiredPlan;
    private Integer creditCost;
    private String creditType;
    private String category;
    private Integer contextLength;
    private boolean active = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
