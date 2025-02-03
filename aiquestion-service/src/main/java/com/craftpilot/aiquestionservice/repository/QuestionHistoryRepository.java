package com.craftpilot.aiquestionservice.repository;

import com.craftpilot.aiquestionservice.model.QuestionHistory;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface QuestionHistoryRepository {
    Mono<QuestionHistory> save(QuestionHistory history);
    Mono<QuestionHistory> findById(String id);
    Flux<QuestionHistory> findAll();
    Mono<Void> deleteById(String id);
    Flux<QuestionHistory> findByUserId(String userId);
    Flux<QuestionHistory> findByQuestionId(String questionId);
} 