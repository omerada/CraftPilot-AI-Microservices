package com.craftpilot.analyticsservice.repository;

import com.craftpilot.analyticsservice.model.PerformanceMetrics;
import com.google.cloud.firestore.Firestore;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.WriteResult;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.DocumentSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Repository
@RequiredArgsConstructor
public class PerformanceMetricsRepository {
    private static final String COLLECTION_NAME = "performance_metrics";
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

    public Mono<PerformanceMetrics> save(PerformanceMetrics metrics) {
        if (metrics.getId() == null) {
            metrics.setId(firestore.collection(COLLECTION_NAME).document().getId());
            metrics.setCreatedAt(LocalDateTime.now());
        }
        metrics.setUpdatedAt(LocalDateTime.now());

        ApiFuture<WriteResult> future = firestore.collection(COLLECTION_NAME)
                .document(metrics.getId())
                .set(metrics);

        return Mono.fromFuture(toCompletableFuture(future).thenApply(writeResult -> metrics));
    }

    public Mono<PerformanceMetrics> findById(String id) {
        ApiFuture<DocumentSnapshot> future = firestore.collection(COLLECTION_NAME)
                .document(id)
                .get();

        return Mono.fromFuture(toCompletableFuture(future)
                .thenApply(documentSnapshot -> documentSnapshot.toObject(PerformanceMetrics.class)));
    }

    public Flux<PerformanceMetrics> findByModelId(String modelId) {
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("modelId", modelId)
                .get();

        return Mono.fromFuture(toCompletableFuture(future))
                .flatMapMany(querySnapshot -> Flux.fromIterable(querySnapshot.toObjects(PerformanceMetrics.class)));
    }

    public Flux<PerformanceMetrics> findByServiceId(String serviceId) {
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("serviceId", serviceId)
                .get();

        return Mono.fromFuture(toCompletableFuture(future))
                .flatMapMany(querySnapshot -> Flux.fromIterable(querySnapshot.toObjects(PerformanceMetrics.class)));
    }

    public Flux<PerformanceMetrics> findByType(PerformanceMetrics.MetricType type) {
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("type", type)
                .get();

        return Mono.fromFuture(toCompletableFuture(future))
                .flatMapMany(querySnapshot -> Flux.fromIterable(querySnapshot.toObjects(PerformanceMetrics.class)));
    }

    public Flux<PerformanceMetrics> findByTimeRange(LocalDateTime start, LocalDateTime end) {
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME)
                .whereGreaterThanOrEqualTo("timestamp", start)
                .whereLessThanOrEqualTo("timestamp", end)
                .get();

        return Mono.fromFuture(toCompletableFuture(future))
                .flatMapMany(querySnapshot -> Flux.fromIterable(querySnapshot.toObjects(PerformanceMetrics.class)));
    }

    public Mono<Void> deleteById(String id) {
        ApiFuture<WriteResult> future = firestore.collection(COLLECTION_NAME)
                .document(id)
                .delete();

        return Mono.fromFuture(toCompletableFuture(future)).then();
    }
} 