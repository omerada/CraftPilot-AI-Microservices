package com.craftpilot.aiquestionservice.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Question {
    private String id;
    private String userId;
    private String content;
    private String context;
    private QuestionType type;
    private QuestionStatus status;
    private DifficultyLevel difficultyLevel;
    private List<String> tags;
    private List<String> options;
    private String correctAnswer;
    private String explanation;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum QuestionType {
        MULTIPLE_CHOICE,
        TRUE_FALSE,
        OPEN_ENDED,
        CODING
    }

    public enum QuestionStatus {
        DRAFT,
        PENDING_REVIEW,
        APPROVED,
        REJECTED
    }

    public enum DifficultyLevel {
        BEGINNER,
        INTERMEDIATE,
        ADVANCED,
        EXPERT
    }
} 