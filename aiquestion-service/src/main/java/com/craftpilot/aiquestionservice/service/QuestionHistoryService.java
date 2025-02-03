package com.craftpilot.aiquestionservice.service;
 
import com.craftpilot.aiquestionservice.model.Question;
import com.craftpilot.aiquestionservice.model.QuestionHistory;
import com.craftpilot.aiquestionservice.repository.FirestoreQuestionHistoryRepository;
import com.google.cloud.Timestamp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionHistoryService {
    private final FirestoreQuestionHistoryRepository questionHistoryRepository;

    public Mono<QuestionHistory> createQuestionHistory(Question question) {
        QuestionHistory history = QuestionHistory.builder()
                .id(UUID.randomUUID().toString())
                .questionId(question.getId())
                .userId(question.getUserId())
                .content(question.getContent())
                .response(question.getResponse())
                .processingTime(question.getProcessingTime())
                .tokenCount(question.getTokenCount())
                .cost(question.getCost())
                .createdAt(Timestamp.now())
                .updatedAt(Timestamp.now())
                .build();

        return questionHistoryRepository.save(history)
                .doOnSuccess(savedHistory -> log.info("Question history created successfully with ID: {}", savedHistory.getId()))
                .doOnError(error -> log.error("Error creating question history: {}", error.getMessage()));
    }

    public Mono<QuestionHistory> getQuestionHistory(String id) {
        return questionHistoryRepository.findById(id)
                .doOnSuccess(history -> {
                    if (history != null) {
                        log.info("Question history found with ID: {}", id);
                    } else {
                        log.info("No question history found with ID: {}", id);
                    }
                })
                .doOnError(error -> log.error("Error retrieving question history: {}", error.getMessage()));
    }

    public Flux<QuestionHistory> getQuestionHistoriesByQuestionId(String questionId) {
        return questionHistoryRepository.findByQuestionId(questionId)
                .doOnComplete(() -> log.info("Retrieved all histories for question ID: {} successfully", questionId))
                .doOnError(error -> log.error("Error retrieving histories for question ID {}: {}", questionId, error.getMessage()));
    }

    public Mono<Void> deleteQuestionHistory(String id) {
        return questionHistoryRepository.deleteById(id)
                .doOnSuccess(result -> log.info("Question history deleted successfully with ID: {}", id))
                .doOnError(error -> log.error("Error deleting question history: {}", error.getMessage()));
    }

    public Flux<QuestionHistory> getUserQuestionHistories(String userId) {
        return questionHistoryRepository.findByUserId(userId)
                .doOnComplete(() -> log.info("Retrieved question histories for user: {}", userId))
                .doOnError(error -> log.error("Error retrieving question histories for user {}: {}", userId, error.getMessage()));
    }

    public Flux<QuestionHistory> getAllQuestionHistories() {
        return questionHistoryRepository.findAll()
                .doOnComplete(() -> log.info("Retrieved all question histories"))
                .doOnError(error -> log.error("Error retrieving all question histories: {}", error.getMessage()));
    }

    public Mono<Void> deleteUserQuestionHistory(String userId) {
        return questionHistoryRepository.deleteByUserId(userId)
                .doOnSuccess(v -> log.info("Deleted question history for user: {}", userId))
                .doOnError(error -> log.error("Error deleting question history for user {}: {}", userId, error.getMessage()));
    }
} 