package com.craftpilot.userservice.service;

import com.craftpilot.userservice.model.UserPreferenceResponse;
import com.google.cloud.firestore.Firestore; 
import com.google.api.core.ApiFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserPreferenceService {

    private final Firestore firestore;
    private static final String COLLECTION_NAME = "user_preferences";

    public Mono<UserPreferenceResponse> getUserPreferences(String userId) {
        return Mono.fromFuture(() -> 
            toCompletableFuture(firestore.collection(COLLECTION_NAME)
                .document(userId)
                .get())
        ).map(doc -> doc.exists() ? 
            doc.toObject(UserPreferenceResponse.class) : 
            createDefaultPreferences(userId));
    }

    public Mono<UserPreferenceResponse> updateUserPreferences(String userId, UserPreferenceResponse preferences) {
        preferences.setUserId(userId);
        return Mono.fromFuture(() -> 
            toCompletableFuture(firestore.collection(COLLECTION_NAME)
                .document(userId)
                .set(preferences))
        ).thenReturn(preferences);
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

    private UserPreferenceResponse createDefaultPreferences(String userId) {
        return UserPreferenceResponse.builder()
            .userId(userId)
            .language("tr")
            .timezone("Europe/Istanbul")
            .emailEnabled(true)
            .pushEnabled(true)
            .smsEnabled(false)
            .quietHoursStart(22L)
            .quietHoursEnd(7L)
            .build();
    }
} 