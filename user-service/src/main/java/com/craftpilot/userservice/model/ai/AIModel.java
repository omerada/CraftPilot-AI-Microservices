package com.craftpilot.userservice.model.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "aiModels")
public class AIModel {
    
    @Id
    private String id;
    
    @Indexed
    private String modelId;
    
    @Indexed
    private String providerId;
    
    private String provider;
    private String modelName;
    private String name;
    private String description;
    private ModelType type;
    
    private int contextLength;
    private int inputCostPerToken;
    private int outputCostPerToken;
    private Integer maxInputTokens;
    private Integer creditCost;
    private String creditType;
    private String category;
    private String requiredPlan;
    
    private List<String> supportedLanguages;
    private List<String> tags;
    private Map<String, Object> capabilities;
    
    @Builder.Default
    private boolean enabled = true;
    
    @Builder.Default
    private Instant createdAt = Instant.now();
    
    @Builder.Default
    private Instant updatedAt = Instant.now();
    
    public enum ModelType {
        TEXT, IMAGE, AUDIO, MULTIMODAL
    }
}
