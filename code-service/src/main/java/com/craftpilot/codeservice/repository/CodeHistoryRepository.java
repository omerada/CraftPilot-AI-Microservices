package com.craftpilot.codeservice.repository;

import com.craftpilot.codeservice.model.CodeHistory;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

@Repository
@RequiredArgsConstructor
public class CodeHistoryRepository {
    private static final String COLLECTION_NAME = "code_histories";
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

    public Mono<CodeHistory> save(CodeHistory codeHistory) {
        if (codeHistory.getId() == null) {
            codeHistory.setId(firestore.collection(COLLECTION_NAME).document().getId());
        }
        if (codeHistory.getCreatedAt() == null) {
            codeHistory.setCreatedAt(Instant.now());
        }
        codeHistory.setUpdatedAt(Instant.now());

        ApiFuture<WriteResult> future = firestore.collection(COLLECTION_NAME)
                .document(codeHistory.getId())
                .set(codeHistory);

        return Mono.fromFuture(toCompletableFuture(future))
                .thenReturn(codeHistory);
    }

    public Mono<CodeHistory> findById(String id) {
        ApiFuture<DocumentSnapshot> future = firestore.collection(COLLECTION_NAME)
                .document(id)
                .get();

        return Mono.fromFuture(toCompletableFuture(future))
                .map(doc -> doc.exists() ? doc.toObject(CodeHistory.class) : null);
    }

    public Flux<CodeHistory> findByUserId(String userId) {
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .orderBy("createdAt")
                .get();

        return Mono.fromFuture(toCompletableFuture(future))
                .flatMapMany(querySnapshot -> Flux.fromIterable(querySnapshot.toObjects(CodeHistory.class)));
    }

    public Mono<Void> deleteById(String id) {
        ApiFuture<WriteResult> future = firestore.collection(COLLECTION_NAME)
                .document(id)
                .delete();

        return Mono.fromFuture(toCompletableFuture(future))
                .then();
    }
} 