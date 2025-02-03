package com.craftpilot.adminservice.repository;

import com.craftpilot.adminservice.model.AdminAction;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Repository
public class AdminActionRepository {
    private final Firestore firestore;
    private static final String COLLECTION_NAME = "admin_actions";

    public AdminActionRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    public Mono<AdminAction> save(AdminAction action) {
        if (action.getId() == null) {
            action.setId(firestore.collection(COLLECTION_NAME).document().getId());
        }
        action.setTimestamp(LocalDateTime.now());
        ApiFuture<WriteResult> future = firestore.collection(COLLECTION_NAME)
                .document(action.getId())
                .set(action);
        return Mono.fromFuture(toCompletableFuture(future))
                .thenReturn(action);
    }

    public Flux<AdminAction> findAll() {
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME).get();
        return Mono.fromFuture(toCompletableFuture(future))
                .flatMapMany(querySnapshot -> Flux.fromIterable(querySnapshot.getDocuments()))
                .map(doc -> doc.toObject(AdminAction.class));
    }

    public Mono<AdminAction> findById(String id) {
        ApiFuture<DocumentSnapshot> future = firestore.collection(COLLECTION_NAME)
                .document(id)
                .get();
        return Mono.fromFuture(toCompletableFuture(future))
                .map(doc -> doc.toObject(AdminAction.class));
    }

    public Flux<AdminAction> findByAdminId(String adminId) {
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("adminId", adminId)
                .get();
        return Mono.fromFuture(toCompletableFuture(future))
                .flatMapMany(querySnapshot -> Flux.fromIterable(querySnapshot.getDocuments()))
                .map(doc -> doc.toObject(AdminAction.class));
    }

    public Flux<AdminAction> findByActionType(AdminAction.ActionType actionType) {
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("actionType", actionType)
                .get();
        return Mono.fromFuture(toCompletableFuture(future))
                .flatMapMany(querySnapshot -> Flux.fromIterable(querySnapshot.getDocuments()))
                .map(doc -> doc.toObject(AdminAction.class));
    }

    public Flux<AdminAction> findByStatus(AdminAction.ActionStatus status) {
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("status", status)
                .get();
        return Mono.fromFuture(toCompletableFuture(future))
                .flatMapMany(querySnapshot -> Flux.fromIterable(querySnapshot.getDocuments()))
                .map(doc -> doc.toObject(AdminAction.class));
    }

    public Flux<AdminAction> findByTimeRange(LocalDateTime start, LocalDateTime end) {
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME)
                .whereGreaterThanOrEqualTo("timestamp", start)
                .whereLessThanOrEqualTo("timestamp", end)
                .get();
        return Mono.fromFuture(toCompletableFuture(future))
                .flatMapMany(querySnapshot -> Flux.fromIterable(querySnapshot.getDocuments()))
                .map(doc -> doc.toObject(AdminAction.class));
    }

    public Flux<AdminAction> findByTargetId(String targetId) {
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("targetId", targetId)
                .get();
        return Mono.fromFuture(toCompletableFuture(future))
                .flatMapMany(querySnapshot -> Flux.fromIterable(querySnapshot.getDocuments()))
                .map(doc -> doc.toObject(AdminAction.class));
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