package com.craftpilot.subscriptionservice.repository;

import com.craftpilot.subscriptionservice.model.subscription.entity.Plan;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface PlanRepository extends ReactiveMongoRepository<Plan, String> {

    Flux<Plan> findByIsDeletedFalse();

    Flux<Plan> findByIsActiveTrueAndIsDeletedFalse();
}