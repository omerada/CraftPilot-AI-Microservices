package com.craftpilot.aiquestionservice.model;

import com.craftpilot.aiquestionservice.model.enums.QuestionStatus;
import com.craftpilot.aiquestionservice.model.enums.QuestionType;
import com.google.cloud.Timestamp;
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
public class Question {
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
} 