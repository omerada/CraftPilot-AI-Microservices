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
    private String userId;
    private String eventType;
    private String requestId;
    private String model;
    private String prompt;
    private LocalDateTime timestamp;
    private AIRequest request;
    private AIResponse response;
    private String error;

    public static AIEvent fromRequest(AIRequest request, AIResponse response, String eventType) {
        return AIEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .userId(request.getUserId())
                .eventType(eventType)
                .requestId(request.getRequestId())
                .model(request.getModel())
                .prompt(request.getPrompt())
                .timestamp(LocalDateTime.now())
                .request(request)
                .response(response)
                .build();
    }

    public static AIEvent error(AIRequest request, String error) {
        return AIEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .userId(request.getUserId())
                .eventType("AI_ERROR")
                .requestId(request.getRequestId())
                .model(request.getModel())
                .prompt(request.getPrompt())
                .timestamp(LocalDateTime.now())
                .request(request)
                .error(error)
                .build();
    }
}