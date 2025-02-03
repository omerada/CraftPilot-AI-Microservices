package com.craftpilot.aiquestionservice.controller.dto;

import com.craftpilot.aiquestionservice.model.enums.QuestionType;
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
public class QuestionRequest {
    private String content;
    private String context;
    private String question;
    private QuestionType type;
    private String modelId;
    private Map<String, Object> parameters;
    private Map<String, Object> preferences;
    private List<String> tags;
    private Boolean useWebSearch;
} 