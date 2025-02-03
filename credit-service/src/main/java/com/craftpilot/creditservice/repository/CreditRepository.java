package com.craftpilot.creditservice.repository;

import com.craftpilot.creditservice.model.Credit;
import com.google.cloud.firestore.Firestore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

@Repository
@RequiredArgsConstructor
public class CreditRepository {
    private final Firestore firestore;
    private static final String COLLECTION_NAME = "credits";

    public Mono<Credit> findByUserId(String userId) {
        return Mono.fromFuture(() -> {
            CompletableFuture<Credit> future = new CompletableFuture<>();
            firestore.collection(COLLECTION_NAME)
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("deleted", false)
                    .get()
                    .addListener(() -> {
                        try {
                            var querySnapshot = firestore.collection(COLLECTION_NAME)
                                    .whereEqualTo("userId", userId)
                                    .whereEqualTo("deleted", false)
                                    .get()
                                    .get();
                            var credit = querySnapshot.getDocuments().stream()
                                    .findFirst()
                                    .map(document -> document.toObject(Credit.class))
                                    .orElse(null);
                            future.complete(credit);
                        } catch (Exception e) {
                            future.completeExceptionally(e);
                        }
                    }, Runnable::run);
            return future;
        });
    }

    public Mono<Credit> save(Credit credit) {
        if (credit.getId() == null) {
            credit.setId(firestore.collection(COLLECTION_NAME).document().getId());
        }
        return Mono.fromFuture(() -> {
            CompletableFuture<Credit> future = new CompletableFuture<>();
            firestore.collection(COLLECTION_NAME)
                    .document(credit.getId())
                    .set(credit)
                    .addListener(() -> future.complete(credit), Runnable::run);
            return future;
        });
    }

    public Mono<Credit> findById(String id) {
        return Mono.fromFuture(() -> {
            CompletableFuture<Credit> future = new CompletableFuture<>();
            firestore.collection(COLLECTION_NAME)
                    .document(id)
                    .get()
                    .addListener(() -> {
                        try {
                            var documentSnapshot = firestore.collection(COLLECTION_NAME)
                                    .document(id)
                                    .get()
                                    .get();
                            future.complete(documentSnapshot.toObject(Credit.class));
                        } catch (Exception e) {
                            future.completeExceptionally(e);
                        }
                    }, Runnable::run);
            return future;
        });
    }
} 