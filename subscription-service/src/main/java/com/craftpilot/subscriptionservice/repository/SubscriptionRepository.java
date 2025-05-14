package com.craftpilot.subscriptionservice.repository;

import com.craftpilot.subscriptionservice.model.subscription.entity.Subscription;
import com.craftpilot.subscriptionservice.model.subscription.enums.SubscriptionStatus;
import com.craftpilot.subscriptionservice.model.subscription.enums.SubscriptionType;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
public interface SubscriptionRepository extends ReactiveMongoRepository<Subscription, String> {
    Flux<Subscription> findByUserId(String userId);

    Mono<Subscription> findByUserIdAndActiveTrue(String userId);

    Flux<Subscription> findByEndDateBeforeAndActiveTrue(LocalDateTime date);

    Flux<Subscription> findByStatus(SubscriptionStatus status);

    Flux<Subscription> findByType(SubscriptionType type);

    Flux<Subscription> findByEndDateBefore(LocalDateTime date);

    Flux<Subscription> findByAutoRenewalIsTrue();
}