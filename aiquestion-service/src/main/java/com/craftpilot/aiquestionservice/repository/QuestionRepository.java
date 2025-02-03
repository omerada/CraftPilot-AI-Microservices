package com.craftpilot.aiquestionservice.repository;

import com.craftpilot.aiquestionservice.model.Question;
import com.craftpilot.aiquestionservice.model.enums.QuestionStatus;
import com.craftpilot.aiquestionservice.model.enums.QuestionType;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface QuestionRepository {
    Mono<Question> save(Question question);
    Mono<Question> findById(String id);
    Flux<Question> findAll();
    Mono<Void> deleteById(String id);
    Flux<Question> findByUserId(String userId);
    Flux<Question> findByType(QuestionType type);
    Flux<Question> findByStatus(QuestionStatus status);
    Flux<Question> findByTagsContaining(String tag);
} 