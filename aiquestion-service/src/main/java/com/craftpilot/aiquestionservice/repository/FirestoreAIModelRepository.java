package com.craftpilot.aiquestionservice.repository;

import com.craftpilot.aiquestionservice.model.AIModel;
import com.craftpilot.aiquestionservice.model.enums.ModelType;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

@Repository
@RequiredArgsConstructor
public class FirestoreAIModelRepository implements AIModelRepository {
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

    @Override
    public Mono<AIModel> save(AIModel model) {
        ApiFuture<WriteResult> future = firestore.collection(COLLECTION_NAME).document(model.getId()).set(model);
        return Mono.fromFuture(() -> toCompletableFuture(future))
                .thenReturn(model);
    }

    @Override
    public Mono<AIModel> findById(String id) {
        ApiFuture<DocumentSnapshot> future = firestore.collection(COLLECTION_NAME).document(id).get();
        return Mono.fromFuture(() -> toCompletableFuture(future))
                .map(doc -> doc.exists() ? doc.toObject(AIModel.class) : null);
    }

    @Override
    public Flux<AIModel> findAll() {
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME).get();
        return Mono.fromFuture(() -> toCompletableFuture(future))
                .flatMapMany(querySnapshot -> Flux.fromIterable(querySnapshot.getDocuments()))
                .map(doc -> doc.toObject(AIModel.class));
    }

    @Override
    public Mono<Void> deleteById(String id) {
        ApiFuture<WriteResult> future = firestore.collection(COLLECTION_NAME).document(id).delete();
        return Mono.fromFuture(() -> toCompletableFuture(future))
                .then();
    }

    @Override
    public Flux<AIModel> findByIsActiveTrue() {
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("isActive", true)
                .get();
        return Mono.fromFuture(() -> toCompletableFuture(future))
                .flatMapMany(querySnapshot -> Flux.fromIterable(querySnapshot.getDocuments()))
                .map(doc -> doc.toObject(AIModel.class));
    }

    @Override
    public Mono<AIModel> findByTypeAndIsActiveTrue(ModelType type) {
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("type", type)
                .whereEqualTo("isActive", true)
                .limit(1)
                .get();
        return Mono.fromFuture(() -> toCompletableFuture(future))
                .map(querySnapshot -> querySnapshot.getDocuments().isEmpty() ? null : querySnapshot.getDocuments().get(0).toObject(AIModel.class));
    }
} 