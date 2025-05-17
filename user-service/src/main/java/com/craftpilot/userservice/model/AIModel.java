package com.craftpilot.userservice.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
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
    
    @JsonIgnore
    private String providerId;
    
    private int maxInputTokens;
    private String requiredPlan;
    private int creditCost;
    private String creditType;
    private String category;
    private int contextLength;
    private boolean active = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
