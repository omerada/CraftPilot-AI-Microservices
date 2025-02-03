package com.craftpilot.subscriptionservice.repository;

import com.craftpilot.subscriptionservice.model.subscription.entity.Plan;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PlanRepository {
    Mono<Plan> save(Plan plan);
    Mono<Plan> findById(String id);
    Flux<Plan> findAll();
    Flux<Plan> findAllActivePlans();
    Mono<Void> delete(String id);
} 