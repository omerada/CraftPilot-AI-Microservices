package com.craftpilot.userservice.repository;

import com.craftpilot.userservice.model.ai.AIModel;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface AIModelRepository extends ReactiveMongoRepository<AIModel, String> {
    Mono<AIModel> findByModelId(String modelId);
    Flux<AIModel> findByProvider(String provider);
    Flux<AIModel> findByCategory(String category);
}
