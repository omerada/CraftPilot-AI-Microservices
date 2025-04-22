package com.craftpilot.userservice.repository;

import com.craftpilot.userservice.model.ai.Provider;
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
public class ProviderRepository {
    
    private final Firestore firestore;
    private static final String COLLECTION_NAME = "providers";

    public Flux<Provider> findAll() {
        return Mono.fromCallable(() -> 
            firestore.collection(COLLECTION_NAME)
                    .get()
                    .get()
                    .getDocuments()
                    .stream()
                    .map(doc -> doc.toObject(Provider.class))
                    .toList()
        )
        .subscribeOn(Schedulers.boundedElastic())
        .flatMapMany(Flux::fromIterable);
    }

    public Mono<Provider> findById(String name) {
        return Mono.fromCallable(() -> 
            firestore.collection(COLLECTION_NAME)
                    .document(name)
                    .get()
                    .get()
                    .toObject(Provider.class)
        ).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Provider> save(Provider provider) {
        return Mono.fromCallable(() -> {
            firestore.collection(COLLECTION_NAME).document(provider.getName()).set(provider).get();
            return provider;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Void> deleteById(String name) {
        return Mono.fromRunnable(() -> {
            try {
                firestore.collection(COLLECTION_NAME).document(name).delete().get();
            } catch (Exception e) {
                throw new RuntimeException("Error deleting provider", e);
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
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
