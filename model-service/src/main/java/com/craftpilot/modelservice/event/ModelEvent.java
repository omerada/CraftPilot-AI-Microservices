package com.craftpilot.modelservice.event;

import com.craftpilot.modelservice.model.AIModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelEvent {
    private String eventId;
    private String eventType;
    private String modelId;
    private AIModel.ModelType modelType;
    private AIModel.ModelStatus status;
    private LocalDateTime timestamp;
} 