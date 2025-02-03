package com.craftpilot.modelservice.repository;

import com.craftpilot.modelservice.model.AIModel;
import com.google.cloud.firestore.Firestore;
import com.google.api.core.ApiFuture; 
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Repository
@RequiredArgsConstructor
public class AIModelRepository {
    private static final String COLLECTION_NAME = "ai_models";
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

    public Mono<AIModel> save(AIModel model) {
        if (model.getId() == null) {
            model.setId(firestore.collection(COLLECTION_NAME).document().getId());
            model.setCreatedAt(LocalDateTime.now());
        }
        model.setUpdatedAt(LocalDateTime.now());

        ApiFuture<com.google.cloud.firestore.WriteResult> apiFuture = firestore.collection(COLLECTION_NAME)
                .document(model.getId())
                .set(model);

        return Mono.fromFuture(toCompletableFuture(apiFuture).thenApply(result -> model));
    }

    public Mono<AIModel> findById(String id) {
        ApiFuture<com.google.cloud.firestore.DocumentSnapshot> apiFuture = firestore.collection(COLLECTION_NAME)
                .document(id)
                .get();

        return Mono.fromFuture(toCompletableFuture(apiFuture)
                .thenApply(snapshot -> snapshot.toObject(AIModel.class)));
    }

    public Flux<AIModel> findAll() {
        ApiFuture<com.google.cloud.firestore.QuerySnapshot> apiFuture = firestore.collection(COLLECTION_NAME).get();

        return Mono.fromFuture(toCompletableFuture(apiFuture)
                .thenApply(querySnapshot -> querySnapshot.toObjects(AIModel.class)))
                .flatMapMany(Flux::fromIterable);
    }

    public Flux<AIModel> findByType(AIModel.ModelType type) {
        ApiFuture<com.google.cloud.firestore.QuerySnapshot> apiFuture = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("type", type)
                .get();

        return Mono.fromFuture(toCompletableFuture(apiFuture)
                .thenApply(querySnapshot -> querySnapshot.toObjects(AIModel.class)))
                .flatMapMany(Flux::fromIterable);
    }

    public Flux<AIModel> findByProvider(String provider) {
        ApiFuture<com.google.cloud.firestore.QuerySnapshot> apiFuture = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("provider", provider)
                .get();

        return Mono.fromFuture(toCompletableFuture(apiFuture)
                .thenApply(querySnapshot -> querySnapshot.toObjects(AIModel.class)))
                .flatMapMany(Flux::fromIterable);
    }

    public Flux<AIModel> findByStatus(AIModel.ModelStatus status) {
        ApiFuture<com.google.cloud.firestore.QuerySnapshot> apiFuture = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("status", status)
                .get();

        return Mono.fromFuture(toCompletableFuture(apiFuture)
                .thenApply(querySnapshot -> querySnapshot.toObjects(AIModel.class)))
                .flatMapMany(Flux::fromIterable);
    }

    public Mono<Void> deleteById(String id) {
        ApiFuture<com.google.cloud.firestore.WriteResult> apiFuture = firestore.collection(COLLECTION_NAME)
                .document(id)
                .delete();

        return Mono.fromFuture(toCompletableFuture(apiFuture).thenApply(result -> null));
    }
} 