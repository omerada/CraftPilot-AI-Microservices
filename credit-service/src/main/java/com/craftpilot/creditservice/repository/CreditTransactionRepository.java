package com.craftpilot.creditservice.repository;

import com.craftpilot.creditservice.model.CreditTransaction;
import com.google.cloud.firestore.Firestore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Repository
@RequiredArgsConstructor
public class CreditTransactionRepository {
    private final Firestore firestore;
    private static final String COLLECTION_NAME = "credit_transactions";

    public Mono<CreditTransaction> save(CreditTransaction transaction) {
        if (transaction.getId() == null) {
            transaction.setId(firestore.collection(COLLECTION_NAME).document().getId());
        }
        return Mono.fromFuture(() -> {
            CompletableFuture<CreditTransaction> future = new CompletableFuture<>();
            firestore.collection(COLLECTION_NAME)
                    .document(transaction.getId())
                    .set(transaction)
                    .addListener(() -> future.complete(transaction), Runnable::run);
            return future;
        });
    }

    public Flux<CreditTransaction> findByUserId(String userId) {
        return Mono.fromFuture(() -> {
            CompletableFuture<List<CreditTransaction>> future = new CompletableFuture<>();
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
                            var transactions = querySnapshot.getDocuments().stream()
                                    .map(document -> document.toObject(CreditTransaction.class))
                                    .toList();
                            future.complete(transactions);
                        } catch (Exception e) {
                            future.completeExceptionally(e);
                        }
                    }, Runnable::run);
            return future;
        }).flatMapMany(Flux::fromIterable);
    }

    public Mono<CreditTransaction> findById(String id) {
        return Mono.fromFuture(() -> {
            CompletableFuture<CreditTransaction> future = new CompletableFuture<>();
            firestore.collection(COLLECTION_NAME)
                    .document(id)
                    .get()
                    .addListener(() -> {
                        try {
                            var documentSnapshot = firestore.collection(COLLECTION_NAME)
                                    .document(id)
                                    .get()
                                    .get();
                            future.complete(documentSnapshot.toObject(CreditTransaction.class));
                        } catch (Exception e) {
                            future.completeExceptionally(e);
                        }
                    }, Runnable::run);
            return future;
        });
    }

    public Flux<CreditTransaction> findPendingTransactions() {
        return Mono.fromFuture(() -> {
            CompletableFuture<List<CreditTransaction>> future = new CompletableFuture<>();
            firestore.collection(COLLECTION_NAME)
                    .whereEqualTo("status", CreditTransaction.TransactionStatus.PENDING)
                    .whereEqualTo("deleted", false)
                    .get()
                    .addListener(() -> {
                        try {
                            var querySnapshot = firestore.collection(COLLECTION_NAME)
                                    .whereEqualTo("status", CreditTransaction.TransactionStatus.PENDING)
                                    .whereEqualTo("deleted", false)
                                    .get()
                                    .get();
                            var transactions = querySnapshot.getDocuments().stream()
                                    .map(document -> document.toObject(CreditTransaction.class))
                                    .toList();
                            future.complete(transactions);
                        } catch (Exception e) {
                            future.completeExceptionally(e);
                        }
                    }, Runnable::run);
            return future;
        }).flatMapMany(Flux::fromIterable);
    }
} 