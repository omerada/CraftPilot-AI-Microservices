package com.craftpilot.userservice.repository;

import com.craftpilot.userservice.model.UserPreference;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.CompletableFuture;

@Repository
@RequiredArgsConstructor
public class UserPreferenceRepository {

    private final Firestore firestore;
    private static final String COLLECTION_NAME = "userPreferences";

    public Mono<UserPreference> findById(String userId) {
        return Mono.fromCallable(() -> 
            firestore.collection(COLLECTION_NAME)
                    .document(userId)
                    .get()
                    .get()
                    .toObject(UserPreference.class)
        ).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<UserPreference> save(UserPreference preference) {
        return Mono.fromCallable(() -> {
            ApiFuture<WriteResult> future = firestore.collection(COLLECTION_NAME)
                    .document(preference.getUserId())
                    .set(preference);
            
            future.get(); // Bloklama işlemi boundedElastic scheduler'da yapılıyor
            return preference;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Void> deleteById(String userId) {
        return Mono.fromRunnable(() -> {
            try {
                firestore.collection(COLLECTION_NAME).document(userId).delete().get();
            } catch (Exception e) {
                throw new RuntimeException("Error deleting user preference", e);
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
