package com.craftpilot.aiquestionservice.repository;

import com.craftpilot.aiquestionservice.model.AIModel;
import com.craftpilot.aiquestionservice.model.enums.ModelType;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface AIModelRepository {
    Mono<AIModel> save(AIModel model);
    Mono<AIModel> findById(String id);
    Flux<AIModel> findAll();
    Mono<Void> deleteById(String id);
    Flux<AIModel> findByIsActiveTrue();
    Mono<AIModel> findByTypeAndIsActiveTrue(ModelType type);
} 