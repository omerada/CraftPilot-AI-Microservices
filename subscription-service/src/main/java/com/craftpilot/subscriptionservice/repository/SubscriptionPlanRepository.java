package com.craftpilot.subscriptionservice.repository;

import com.craftpilot.subscriptionservice.model.subscription.entity.SubscriptionPlan;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface SubscriptionPlanRepository extends ReactiveMongoRepository<SubscriptionPlan, String> {

    Flux<SubscriptionPlan> findByIsDeletedFalse();

    Flux<SubscriptionPlan> findByIsActiveTrueAndIsDeletedFalse();

    default Flux<SubscriptionPlan> findAllActive() {
        return findByIsActiveTrueAndIsDeletedFalse();
    }
}