package com.craftpilot.aiquestionservice.event;

import com.craftpilot.aiquestionservice.model.Question;
import com.craftpilot.aiquestionservice.model.enums.QuestionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionEvent {
    private String eventId;
    private QuestionEventType eventType;
    private String questionId;
    private String userId;
    private QuestionStatus status;
    private LocalDateTime timestamp;

    public static QuestionEvent fromQuestion(QuestionEventType eventType, Question question) {
        return QuestionEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .eventType(eventType)
                .questionId(question.getId())
                .userId(question.getUserId())
                .status(question.getStatus())
                .timestamp(LocalDateTime.now())
                .build();
    }
} 