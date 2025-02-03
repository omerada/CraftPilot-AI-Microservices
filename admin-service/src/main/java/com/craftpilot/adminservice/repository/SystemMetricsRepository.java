package com.craftpilot.adminservice.repository;

import com.craftpilot.adminservice.model.SystemMetrics;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

@Repository
public class SystemMetricsRepository {
    private final Firestore firestore;
    private static final String COLLECTION_NAME = "system_metrics";

    public SystemMetricsRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    public Mono<SystemMetrics> save(SystemMetrics metrics) {
        ApiFuture<WriteResult> future = firestore.collection(COLLECTION_NAME)
                .document(metrics.getServiceId())
                .set(metrics);
        return Mono.fromFuture(toCompletableFuture(future))
                .thenReturn(metrics);
    }

    public Mono<SystemMetrics> findById(String serviceId) {
        ApiFuture<DocumentSnapshot> future = firestore.collection(COLLECTION_NAME)
                .document(serviceId)
                .get();
        return Mono.fromFuture(toCompletableFuture(future))
                .map(doc -> doc.toObject(SystemMetrics.class));
    }

    public Flux<SystemMetrics> findAll() {
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME)
                .get();
        return Mono.fromFuture(toCompletableFuture(future))
                .flatMapMany(querySnapshot -> Flux.fromIterable(querySnapshot.getDocuments()))
                .map(doc -> doc.toObject(SystemMetrics.class));
    }

    public Flux<SystemMetrics> findByServiceType(SystemMetrics.ServiceType serviceType) {
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("serviceType", serviceType)
                .get();
        return Mono.fromFuture(toCompletableFuture(future))
                .flatMapMany(querySnapshot -> Flux.fromIterable(querySnapshot.getDocuments()))
                .map(doc -> doc.toObject(SystemMetrics.class));
    }

    public Flux<SystemMetrics> findByStatus(SystemMetrics.ServiceStatus status) {
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("status", status)
                .get();
        return Mono.fromFuture(toCompletableFuture(future))
                .flatMapMany(querySnapshot -> Flux.fromIterable(querySnapshot.getDocuments()))
                .map(doc -> doc.toObject(SystemMetrics.class));
    }

    public Mono<Void> deleteById(String serviceId) {
        ApiFuture<WriteResult> future = firestore.collection(COLLECTION_NAME)
                .document(serviceId)
                .delete();
        return Mono.fromFuture(toCompletableFuture(future))
                .then();
    }

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
} 