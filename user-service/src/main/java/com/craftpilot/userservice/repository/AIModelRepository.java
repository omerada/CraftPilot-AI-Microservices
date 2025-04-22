package com.craftpilot.userservice.repository;

import com.craftpilot.userservice.model.ai.AIModel;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface AIModelRepository extends ReactiveCrudRepository<AIModel, String> {
    Flux<AIModel> findByRequiredPlan(String plan);
}
