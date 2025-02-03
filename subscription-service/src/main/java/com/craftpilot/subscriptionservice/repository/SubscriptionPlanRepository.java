package com.craftpilot.subscriptionservice.repository;

import com.craftpilot.subscriptionservice.model.subscription.entity.SubscriptionPlan;
import com.google.cloud.firestore.Firestore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;

@Slf4j
@Repository
@RequiredArgsConstructor
public class SubscriptionPlanRepository {

    private final Firestore firestore;
    private static final String COLLECTION_NAME = "subscription_plans";

    public Mono<SubscriptionPlan> save(SubscriptionPlan plan) {
        if (plan.getId() == null) {
            plan.setId(UUID.randomUUID().toString());
        }
        
        return Mono.fromCallable(() -> {
            firestore.collection(COLLECTION_NAME)
                    .document(plan.getId())
                    .set(plan)
                    .get();
            return plan;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<SubscriptionPlan> findById(String id) {
        return Mono.fromCallable(() ->
            firestore.collection(COLLECTION_NAME)
                    .document(id)
                    .get()
                    .get()
                    .toObject(SubscriptionPlan.class)
        ).subscribeOn(Schedulers.boundedElastic());
    }

    public Flux<SubscriptionPlan> findAll() {
        return Mono.fromCallable(() ->
            firestore.collection(COLLECTION_NAME)
                    .whereEqualTo("isDeleted", false)
                    .get()
                    .get()
                    .getDocuments()
                    .stream()
                    .map(doc -> doc.toObject(SubscriptionPlan.class))
                    .toList()
        )
        .flatMapMany(Flux::fromIterable)
        .subscribeOn(Schedulers.boundedElastic());
    }

    public Flux<SubscriptionPlan> findAllActive() {
        return Mono.fromCallable(() ->
            firestore.collection(COLLECTION_NAME)
                    .whereEqualTo("isActive", true)
                    .whereEqualTo("isDeleted", false)
                    .get()
                    .get()
                    .getDocuments()
                    .stream()
                    .map(doc -> doc.toObject(SubscriptionPlan.class))
                    .toList()
        )
        .flatMapMany(Flux::fromIterable)
        .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Void> deleteById(String id) {
        return Mono.fromRunnable(() -> {
            try {
                firestore.collection(COLLECTION_NAME).document(id).delete().get();
            } catch (Exception e) {
                throw new RuntimeException("Error deleting subscription plan", e);
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }
} 