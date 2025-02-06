package com.craftpilot.llmservice.event;

import com.craftpilot.llmservice.model.AIRequest;
import com.craftpilot.llmservice.model.AIResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIEvent {
    private String eventId;
    private AIEventType eventType;
    private String model;
    private String requestType;
    private Long processingTime;
    private Integer tokenCount;
    private LocalDateTime timestamp;
    private String error;

    public static AIEvent success(AIRequest request, AIResponse response) {
        return AIEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .eventType(AIEventType.COMPLETED)
                .model(request.getModel())
                .requestType(request.getRequestType())
                .processingTime(response.getProcessingTime())
                .tokenCount(response.getTokenCount())
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static AIEvent failure(AIRequest request, String errorMessage) {
        return AIEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .eventType(AIEventType.FAILED)
                .model(request.getModel())
                .requestType(request.getRequestType())
                .error(errorMessage)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static AIEvent started(AIRequest request) {
        return AIEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .eventType(AIEventType.STARTED)
                .model(request.getModel())
                .requestType(request.getRequestType())
                .timestamp(LocalDateTime.now())
                .build();
    }
} 