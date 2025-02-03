package com.craftpilot.codeservice.repository;

import com.craftpilot.codeservice.model.Code;
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
public class CodeRepository {
    private static final String COLLECTION_NAME = "codes";
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

    public Mono<Code> save(Code code) {
        if (code.getId() == null) {
            code.setId(firestore.collection(COLLECTION_NAME).document().getId());
        }
        if (code.getCreatedAt() == null) {
            code.setCreatedAt(Instant.now());
        }
        code.setUpdatedAt(Instant.now());

        ApiFuture<WriteResult> future = firestore.collection(COLLECTION_NAME)
                .document(code.getId())
                .set(code);

        return Mono.fromFuture(toCompletableFuture(future))
                .thenReturn(code);
    }

    public Mono<Code> findById(String id) {
        ApiFuture<DocumentSnapshot> future = firestore.collection(COLLECTION_NAME)
                .document(id)
                .get();

        return Mono.fromFuture(toCompletableFuture(future))
                .map(doc -> doc.exists() ? doc.toObject(Code.class) : null);
    }

    public Flux<Code> findByUserId(String userId) {
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .orderBy("createdAt")
                .get();

        return Mono.fromFuture(toCompletableFuture(future))
                .flatMapMany(querySnapshot -> Flux.fromIterable(querySnapshot.toObjects(Code.class)));
    }

    public Mono<Void> deleteById(String id) {
        ApiFuture<WriteResult> future = firestore.collection(COLLECTION_NAME)
                .document(id)
                .delete();

        return Mono.fromFuture(toCompletableFuture(future))
                .then();
    }
} 