package com.craftpilot.userservice.repository;

import com.craftpilot.userservice.model.ai.AIModel;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.CompletableFuture;

@Repository
@RequiredArgsConstructor
public class AIModelRepository {
    
    private final Firestore firestore;
    private static final String COLLECTION_NAME = "ai_models";

    public Flux<AIModel> findAll() {
        return Mono.fromCallable(() -> 
            firestore.collection(COLLECTION_NAME)
                    .get()
                    .get()
                    .getDocuments()
                    .stream()
                    .map(doc -> doc.toObject(AIModel.class))
                    .toList()
        )
        .subscribeOn(Schedulers.boundedElastic())
        .flatMapMany(Flux::fromIterable);
    }

    public Mono<AIModel> findById(String id) {
        return Mono.fromCallable(() -> 
            firestore.collection(COLLECTION_NAME)
                    .document(id)
                    .get()
                    .get()
                    .toObject(AIModel.class)
        ).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<AIModel> save(AIModel model) {
        return Mono.fromCallable(() -> {
            if (model.getId() == null) {
                model.setId(firestore.collection(COLLECTION_NAME).document().getId());
            }
            firestore.collection(COLLECTION_NAME).document(model.getId()).set(model).get();
            return model;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Void> deleteById(String id) {
        return Mono.fromRunnable(() -> {
            try {
                firestore.collection(COLLECTION_NAME).document(id).delete().get();
            } catch (Exception e) {
                throw new RuntimeException("Error deleting AI model", e);
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    public Flux<AIModel> findByRequiredPlan(String plan) {
        return Mono.fromCallable(() -> 
            firestore.collection(COLLECTION_NAME)
                    .whereEqualTo("requiredPlan", plan)
                    .get()
                    .get()
                    .getDocuments()
                    .stream()
                    .map(doc -> doc.toObject(AIModel.class))
                    .toList()
        )
        .subscribeOn(Schedulers.boundedElastic())
        .flatMapMany(Flux::fromIterable);
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
