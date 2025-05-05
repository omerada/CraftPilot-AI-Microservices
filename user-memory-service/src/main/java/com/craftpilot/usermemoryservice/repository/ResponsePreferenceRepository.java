package com.craftpilot.usermemoryservice.repository;

import com.craftpilot.usermemoryservice.exception.FirebaseAuthException;
import com.craftpilot.usermemoryservice.model.ResponsePreference;
import com.google.api.gax.rpc.PermissionDeniedException;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.ExecutionException;

@Repository
@Slf4j
public class ResponsePreferenceRepository {
    private final Firestore firestore;
    private static final String COLLECTION_NAME = "response_preferences";

    public ResponsePreferenceRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    public Mono<ResponsePreference> findByUserId(String userId) {
        log.info("Fetching response preferences for userId: {}", userId);
        
        return Mono.fromCallable(() -> {
            try {
                DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(userId);
                DocumentSnapshot snapshot = docRef.get().get();
                
                if (snapshot.exists()) {
                    return snapshot.toObject(ResponsePreference.class);
                } else {
                    log.info("No response preferences found for userId: {}", userId);
                    return null;
                }
            } catch (ExecutionException e) {
                if (e.getCause() instanceof PermissionDeniedException) {
                    log.error("Firebase permission denied: {}", e.getCause().getMessage());
                    throw new FirebaseAuthException(
                            "Firebase yetkilendirme hatası: Servis hesabının gerekli izinleri yok", e);
                }
                
                log.error("Error fetching response preferences: {}", e.getMessage(), e);
                throw new RuntimeException("Error fetching response preferences", e);
            } catch (Exception e) {
                log.error("Unexpected error fetching response preferences: {}", e.getMessage(), e);
                throw new RuntimeException("Error accessing response preferences data", e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<ResponsePreference> save(ResponsePreference preference) {
        preference.setId(preference.getUserId()); // userId'yi ID olarak kullan
        
        return Mono.fromCallable(() -> {
            try {
                DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(preference.getId());
                docRef.set(preference).get();
                log.info("Response preferences saved successfully for userId: {}", preference.getUserId());
                return preference;
            } catch (ExecutionException e) {
                if (e.getCause() instanceof PermissionDeniedException) {
                    log.error("Firebase permission denied during save: {}", e.getCause().getMessage());
                    throw new FirebaseAuthException(
                            "Firebase yetkilendirme hatası: Servis hesabının yazma izni yok", e);
                }
                
                log.error("Error saving response preferences: {}", e.getMessage(), e);
                throw new RuntimeException("Error saving response preferences", e);
            } catch (Exception e) {
                log.error("Unexpected error saving response preferences: {}", e.getMessage(), e);
                throw new RuntimeException("Error saving response preferences data", e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Void> deleteByUserId(String userId) {
        return Mono.fromRunnable(() -> {
            try {
                firestore.collection(COLLECTION_NAME).document(userId).delete().get();
                log.info("Response preferences deleted successfully for userId: {}", userId);
            } catch (ExecutionException e) {
                if (e.getCause() instanceof PermissionDeniedException) {
                    log.error("Firebase permission denied during delete: {}", e.getCause().getMessage());
                    throw new FirebaseAuthException(
                            "Firebase yetkilendirme hatası: Servis hesabının silme izni yok", e);
                }
                
                log.error("Error deleting response preferences: {}", e.getMessage(), e);
                throw new RuntimeException("Error deleting response preferences", e);
            } catch (Exception e) {
                log.error("Unexpected error deleting response preferences: {}", e.getMessage(), e);
                throw new RuntimeException("Error deleting response preferences", e);
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }
}
