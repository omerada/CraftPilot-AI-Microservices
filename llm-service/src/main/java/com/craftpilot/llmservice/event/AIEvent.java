package com.craftpilot.llmservice.event;

import com.craftpilot.llmservice.model.AIRequest;
import com.craftpilot.llmservice.model.AIResponse;
import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class AIEvent {
    private String eventId;
    private String requestId;
    private String userId;
    private String model;
    private String prompt;
    private String response;
    private String error;
    private String eventType;
    private Instant timestamp;

    public static AIEvent fromRequest(AIRequest request, AIResponse response, String eventType) {
        return AIEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .requestId(request.getRequestId())
                .userId(request.getUserId())
                .model(request.getModel())
                .prompt(request.getPrompt())
                .response(response != null ? response.getResponse() : null)
                .eventType(eventType)
                .timestamp(Instant.now())
                .build();
    }

    public static AIEvent error(AIRequest request, String errorMessage) {
        return AIEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .requestId(request.getRequestId())
                .userId(request.getUserId())
                .model(request.getModel())
                .prompt(request.getPrompt())
                .error(errorMessage)
                .eventType("ERROR")
                .timestamp(Instant.now())
                .build();
    }
}