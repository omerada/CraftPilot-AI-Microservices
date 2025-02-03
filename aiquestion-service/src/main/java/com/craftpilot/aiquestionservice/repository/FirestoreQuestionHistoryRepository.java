package com.craftpilot.aiquestionservice.repository;

import com.craftpilot.aiquestionservice.model.QuestionHistory; 
import com.google.cloud.firestore.Firestore; 
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
 
import java.util.concurrent.CompletableFuture;

@Repository
@RequiredArgsConstructor
public class FirestoreQuestionHistoryRepository {
    private final Firestore firestore;
    private static final String COLLECTION_NAME = "question_histories";

    private <T> CompletableFuture<T> toCompletableFuture(com.google.api.core.ApiFuture<T> apiFuture) {
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

    public Mono<QuestionHistory> save(QuestionHistory history) {
        return Mono.fromFuture(() -> toCompletableFuture(firestore.collection(COLLECTION_NAME)
                .document(history.getId())
                .set(history))
                .thenApply(result -> history));
    }

    public Mono<QuestionHistory> findById(String id) {
        return Mono.fromFuture(() -> toCompletableFuture(firestore.collection(COLLECTION_NAME)
                .document(id)
                .get())
                .thenApply(document -> document.toObject(QuestionHistory.class)));
    }

    public Flux<QuestionHistory> findByQuestionId(String questionId) {
        return Mono.fromFuture(() -> toCompletableFuture(firestore.collection(COLLECTION_NAME)
                .whereEqualTo("questionId", questionId)
                .get())
                .thenApply(querySnapshot -> querySnapshot.toObjects(QuestionHistory.class)))
                .flatMapMany(Flux::fromIterable);
    }

    public Mono<Void> deleteById(String id) {
        return Mono.fromFuture(() -> toCompletableFuture(firestore.collection(COLLECTION_NAME)
                .document(id)
                .delete())
                .thenApply(result -> null));
    }

    public Flux<QuestionHistory> findByUserId(String userId) {
        return Mono.fromFuture(() -> toCompletableFuture(firestore.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .get())
                .thenApply(querySnapshot -> querySnapshot.toObjects(QuestionHistory.class)))
                .flatMapMany(Flux::fromIterable);
    }

    public Flux<QuestionHistory> findAll() {
        return Mono.fromFuture(() -> toCompletableFuture(firestore.collection(COLLECTION_NAME)
                .get())
                .thenApply(querySnapshot -> querySnapshot.toObjects(QuestionHistory.class)))
                .flatMapMany(Flux::fromIterable);
    }

    public Mono<Void> deleteByUserId(String userId) {
        return findByUserId(userId)
                .flatMap(history -> deleteById(history.getId()))
                .then();
    }
} 