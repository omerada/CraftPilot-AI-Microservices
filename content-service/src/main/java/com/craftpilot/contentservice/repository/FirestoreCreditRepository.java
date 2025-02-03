package com.craftpilot.contentservice.repository;

import com.craftpilot.contentservice.model.Credit;
import com.google.cloud.firestore.Firestore;
import com.google.api.core.ApiFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Repository
@RequiredArgsConstructor
public class FirestoreCreditRepository implements CreditRepository {
    private static final String COLLECTION_NAME = "credits";
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
    public Mono<Credit> save(Credit credit) {
        if (credit.getId() == null) {
            credit.setId(firestore.collection(COLLECTION_NAME).document().getId());
        }
        if (credit.getCreatedAt() == null) {
            credit.setCreatedAt(Instant.now());
        }
        credit.setUpdatedAt(Instant.now());

        return Mono.fromFuture(toCompletableFuture(firestore.collection(COLLECTION_NAME)
                .document(credit.getId())
                .set(credit)))
                .doOnSuccess(result -> log.info("Credit saved successfully: {}", credit.getId()))
                .doOnError(error -> log.error("Error saving credit: {}", error.getMessage()))
                .thenReturn(credit);
    }

    @Override
    public Mono<Credit> findById(String id) {
        return Mono.fromFuture(toCompletableFuture(firestore.collection(COLLECTION_NAME)
                .document(id)
                .get()))
                .map(documentSnapshot -> documentSnapshot.toObject(Credit.class))
                .doOnSuccess(credit -> log.info("Credit retrieved successfully: {}", id))
                .doOnError(error -> log.error("Error retrieving credit: {}", error.getMessage()));
    }

    @Override
    public Mono<Credit> findByUserId(String userId) {
        return Mono.fromFuture(toCompletableFuture(firestore.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .limit(1)
                .get()))
                .map(querySnapshot -> querySnapshot.isEmpty() ? null : querySnapshot.getDocuments().get(0).toObject(Credit.class))
                .doOnSuccess(credit -> log.info("Credit retrieved successfully for user: {}", userId))
                .doOnError(error -> log.error("Error retrieving credit for user: {}", error.getMessage()));
    }

    @Override
    public Mono<Void> deleteById(String id) {
        return Mono.fromFuture(toCompletableFuture(firestore.collection(COLLECTION_NAME)
                .document(id)
                .delete()))
                .doOnSuccess(result -> log.info("Credit deleted successfully: {}", id))
                .doOnError(error -> log.error("Error deleting credit: {}", error.getMessage()))
                .then();
    }

    @Override
    public Mono<Credit> updateCredits(String userId, int credits) {
        return findByUserId(userId)
                .flatMap(existingCredit -> {
                    existingCredit.setCredits(credits);
                    existingCredit.setUpdatedAt(Instant.now());
                    return save(existingCredit);
                });
    }

    @Override
    public Mono<Void> deleteByUserId(String userId) {
        return findByUserId(userId)
                .flatMap(credit -> Mono.fromFuture(toCompletableFuture(firestore.collection(COLLECTION_NAME)
                        .document(credit.getId())
                        .delete()))
                        .doOnSuccess(result -> log.info("Credit deleted successfully: {}", credit.getId()))
                        .doOnError(error -> log.error("Error deleting credit: {}", error.getMessage()))
                        .then());
    }
} 