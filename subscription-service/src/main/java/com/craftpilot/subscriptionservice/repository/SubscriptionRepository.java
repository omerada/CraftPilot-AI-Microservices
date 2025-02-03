package com.craftpilot.subscriptionservice.repository;

import com.craftpilot.subscriptionservice.model.subscription.entity.Subscription;
import com.google.cloud.firestore.Firestore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Repository
@RequiredArgsConstructor
public class SubscriptionRepository {

    private final Firestore firestore;
    private static final String COLLECTION_NAME = "subscriptions";

    public Mono<Subscription> save(Subscription subscription) {
        if (subscription.getId() == null) {
            subscription.setId(UUID.randomUUID().toString());
        }
        
        return Mono.fromCallable(() -> {
            firestore.collection(COLLECTION_NAME)
                    .document(subscription.getId())
                    .set(subscription)
                    .get();
            return subscription;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Subscription> findById(String id) {
        return Mono.fromCallable(() ->
            firestore.collection(COLLECTION_NAME)
                    .document(id)
                    .get()
                    .get()
                    .toObject(Subscription.class)
        ).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Subscription> findByUserIdAndIsActiveTrue(String userId) {
        return Mono.fromCallable(() ->
            firestore.collection(COLLECTION_NAME)
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("isActive", true)
                    .whereEqualTo("isDeleted", false)
                    .get()
                    .get()
                    .getDocuments()
                    .stream()
                    .findFirst()
                    .map(doc -> doc.toObject(Subscription.class))
                    .orElse(null)
        ).subscribeOn(Schedulers.boundedElastic());
    }

    public Flux<Subscription> findByUserId(String userId) {
        return Mono.fromCallable(() ->
            firestore.collection(COLLECTION_NAME)
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("isDeleted", false)
                    .get()
                    .get()
                    .getDocuments()
                    .stream()
                    .map(doc -> doc.toObject(Subscription.class))
                    .toList()
        )
        .flatMapMany(Flux::fromIterable)
        .subscribeOn(Schedulers.boundedElastic());
    }

    public Flux<Subscription> findByEndDateBeforeAndIsActiveTrue(LocalDateTime endDate) {
        return Mono.fromCallable(() ->
            firestore.collection(COLLECTION_NAME)
                    .whereEqualTo("isActive", true)
                    .whereEqualTo("isDeleted", false)
                    .whereLessThan("endDate", endDate)
                    .get()
                    .get()
                    .getDocuments()
                    .stream()
                    .map(doc -> doc.toObject(Subscription.class))
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
                throw new RuntimeException("Error deleting subscription", e);
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }
} 