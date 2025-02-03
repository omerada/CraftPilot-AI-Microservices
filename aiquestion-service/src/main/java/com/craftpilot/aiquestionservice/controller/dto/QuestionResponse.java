package com.craftpilot.aiquestionservice.controller.dto;

import com.craftpilot.aiquestionservice.model.Question;
import com.craftpilot.aiquestionservice.model.enums.QuestionStatus;
import com.craftpilot.aiquestionservice.model.enums.QuestionType;
import com.google.cloud.Timestamp;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuestionResponse {
    private String id;
    private String userId;
    private String content;
    private String context;
    private String question;
    private QuestionType type;
    private QuestionStatus status;
    private String modelId;
    private Map<String, Object> parameters;
    private Map<String, Object> preferences;
    private List<String> tags;
    private Boolean useWebSearch;
    private String response;
    private Long processingTime;
    private Integer tokenCount;
    private Double cost;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private Timestamp completedAt;

    public static QuestionResponse fromModel(Question question) {
        return QuestionResponse.builder()
                .id(question.getId())
                .userId(question.getUserId())
                .content(question.getContent())
                .context(question.getContext())
                .question(question.getQuestion())
                .type(question.getType())
                .status(question.getStatus())
                .modelId(question.getModelId())
                .parameters(question.getParameters())
                .preferences(question.getPreferences())
                .tags(question.getTags())
                .useWebSearch(question.getUseWebSearch())
                .response(question.getResponse())
                .processingTime(question.getProcessingTime())
                .tokenCount(question.getTokenCount())
                .cost(question.getCost())
                .createdAt(question.getCreatedAt())
                .updatedAt(question.getUpdatedAt())
                .completedAt(question.getCompletedAt())
                .build();
    }
} 