package com.craftpilot.userservice.repository;

import com.craftpilot.userservice.model.ai.Provider;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.Firestore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Repository
@RequiredArgsConstructor
public class ProviderRepository {

    private final Firestore firestore;
    private static final String COLLECTION_NAME = "providers";
    
    public Mono<Provider> save(Provider provider) {
        return Mono.fromCallable(() -> {
            try {
                firestore.collection(COLLECTION_NAME)
                        .document(provider.getName())
                        .set(provider)
                        .get();
                return provider;
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException("Error saving provider", e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }
    
    public Flux<Provider> findAll() {
        return Flux.defer(() -> {
            try {
                return Flux.fromIterable(
                        firestore.collection(COLLECTION_NAME).get().get().getDocuments())
                        .map(doc -> doc.toObject(Provider.class));
            } catch (InterruptedException | ExecutionException e) {
                return Flux.error(new RuntimeException("Error retrieving all providers", e));
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Provider> findById(String name) {
        return Mono.fromCallable(() -> {
            try {
                return firestore.collection(COLLECTION_NAME)
                        .document(name)
                        .get()
                        .get()
                        .toObject(Provider.class);
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException("Error retrieving provider by ID", e);
            }
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
