package com.craftpilot.aiquestionservice.model;

import com.google.cloud.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionHistory {
    private String id;
    private String questionId;
    private String userId;
    private String content;
    private String response;
    private Long processingTime;
    private Integer tokenCount;
    private Double cost;
    private Timestamp createdAt;
    private Timestamp updatedAt;
} 