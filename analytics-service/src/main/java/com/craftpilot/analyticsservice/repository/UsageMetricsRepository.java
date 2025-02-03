package com.craftpilot.analyticsservice.repository;

import com.craftpilot.analyticsservice.model.UsageMetrics;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Repository
@RequiredArgsConstructor
public class UsageMetricsRepository {
    private static final String COLLECTION_NAME = "usage_metrics";
    private final Firestore firestore;

    private <T> CompletableFuture<T> toCompletableFuture(ApiFuture<T> apiFuture) {
        CompletableFuture<T> completableFuture = new CompletableFuture<>();
        apiFuture.addListener(() -> {
            try {
                completableFuture.complete(apiFuture.get());
            } catch (Exception e) {
                completableFuture.completeExceptionally(e);
            }
        }, Runnable::run);
        return completableFuture;
    }

    public Mono<UsageMetrics> save(UsageMetrics metrics) {
        if (metrics.getId() == null) {
            metrics.setId(firestore.collection(COLLECTION_NAME).document().getId());
            metrics.setCreatedAt(LocalDateTime.now());
        }
        metrics.setUpdatedAt(LocalDateTime.now());

        ApiFuture<WriteResult> future = firestore.collection(COLLECTION_NAME)
                .document(metrics.getId())
                .set(metrics);

        return Mono.fromFuture(toCompletableFuture(future))
                .thenReturn(metrics);
    }

    public Mono<UsageMetrics> findById(String id) {
        ApiFuture<DocumentSnapshot> future = firestore.collection(COLLECTION_NAME)
                .document(id)
                .get();

        return Mono.fromFuture(toCompletableFuture(future))
                .map(doc -> doc.exists() ? doc.toObject(UsageMetrics.class) : null);
    }

    public Flux<UsageMetrics> findByUserId(String userId) {
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .get();

        return Mono.fromFuture(toCompletableFuture(future))
                .flatMapMany(querySnapshot -> Flux.fromIterable(querySnapshot.toObjects(UsageMetrics.class)));
    }

    public Flux<UsageMetrics> findByServiceType(UsageMetrics.ServiceType serviceType) {
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("serviceType", serviceType)
                .get();

        return Mono.fromFuture(toCompletableFuture(future))
                .flatMapMany(querySnapshot -> Flux.fromIterable(querySnapshot.toObjects(UsageMetrics.class)));
    }

    public Flux<UsageMetrics> findByTimeRange(LocalDateTime start, LocalDateTime end) {
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME)
                .whereGreaterThanOrEqualTo("startTime", start)
                .whereLessThanOrEqualTo("endTime", end)
                .get();

        return Mono.fromFuture(toCompletableFuture(future))
                .flatMapMany(querySnapshot -> Flux.fromIterable(querySnapshot.toObjects(UsageMetrics.class)));
    }

    public Mono<Void> deleteById(String id) {
        ApiFuture<WriteResult> future = firestore.collection(COLLECTION_NAME)
                .document(id)
                .delete();

        return Mono.fromFuture(toCompletableFuture(future))
                .then();
    }
} 