package com.craftpilot.adminservice.repository;

import com.craftpilot.adminservice.model.AuditLog;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Repository
public class AuditLogRepository {
    private final Firestore firestore;
    private static final String COLLECTION_NAME = "audit_logs";

    public AuditLogRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    public Mono<AuditLog> save(AuditLog log) {
        if (log.getId() == null) {
            log.setId(firestore.collection(COLLECTION_NAME).document().getId());
        }
        log.setTimestamp(LocalDateTime.now());
        ApiFuture<WriteResult> future = firestore.collection(COLLECTION_NAME)
                .document(log.getId())
                .set(log);
        return Mono.fromFuture(toCompletableFuture(future))
                .thenReturn(log);
    }

    public Flux<AuditLog> findAll() {
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME).get();
        return Mono.fromFuture(toCompletableFuture(future))
                .flatMapMany(querySnapshot -> Flux.fromIterable(querySnapshot.getDocuments()))
                .map(doc -> doc.toObject(AuditLog.class));
    }

    public Mono<AuditLog> findById(String id) {
        ApiFuture<DocumentSnapshot> future = firestore.collection(COLLECTION_NAME)
                .document(id)
                .get();
        return Mono.fromFuture(toCompletableFuture(future))
                .map(doc -> doc.toObject(AuditLog.class));
    }

    public Flux<AuditLog> findByUserId(String userId) {
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .get();
        return Mono.fromFuture(toCompletableFuture(future))
                .flatMapMany(querySnapshot -> Flux.fromIterable(querySnapshot.getDocuments()))
                .map(doc -> doc.toObject(AuditLog.class));
    }

    public Flux<AuditLog> findByServiceId(String serviceId) {
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("serviceId", serviceId)
                .get();
        return Mono.fromFuture(toCompletableFuture(future))
                .flatMapMany(querySnapshot -> Flux.fromIterable(querySnapshot.getDocuments()))
                .map(doc -> doc.toObject(AuditLog.class));
    }

    public Flux<AuditLog> findByLogType(AuditLog.LogType logType) {
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("logType", logType)
                .get();
        return Mono.fromFuture(toCompletableFuture(future))
                .flatMapMany(querySnapshot -> Flux.fromIterable(querySnapshot.getDocuments()))
                .map(doc -> doc.toObject(AuditLog.class));
    }

    public Flux<AuditLog> findByStatus(AuditLog.LogStatus status) {
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("status", status)
                .get();
        return Mono.fromFuture(toCompletableFuture(future))
                .flatMapMany(querySnapshot -> Flux.fromIterable(querySnapshot.getDocuments()))
                .map(doc -> doc.toObject(AuditLog.class));
    }

    public Flux<AuditLog> findByTimeRange(LocalDateTime start, LocalDateTime end) {
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME)
                .whereGreaterThanOrEqualTo("timestamp", start)
                .whereLessThanOrEqualTo("timestamp", end)
                .get();
        return Mono.fromFuture(toCompletableFuture(future))
                .flatMapMany(querySnapshot -> Flux.fromIterable(querySnapshot.getDocuments()))
                .map(doc -> doc.toObject(AuditLog.class));
    }

    public Flux<AuditLog> findByResource(String resource) {
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("resource", resource)
                .get();
        return Mono.fromFuture(toCompletableFuture(future))
                .flatMapMany(querySnapshot -> Flux.fromIterable(querySnapshot.getDocuments()))
                .map(doc -> doc.toObject(AuditLog.class));
    }

    public Mono<Void> deleteById(String id) {
        ApiFuture<WriteResult> future = firestore.collection(COLLECTION_NAME)
                .document(id)
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