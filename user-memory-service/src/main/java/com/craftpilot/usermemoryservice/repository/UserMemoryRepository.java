package com.craftpilot.usermemoryservice.repository;

import com.craftpilot.usermemoryservice.exception.FirebaseAuthException;
import com.craftpilot.usermemoryservice.model.UserMemory;
import com.google.api.gax.rpc.PermissionDeniedException;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.ExecutionException;

@Repository
@Slf4j
public class UserMemoryRepository {
    private final Firestore firestore;
    private static final String COLLECTION_NAME = "user_memories";

    public UserMemoryRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    public Mono<UserMemory> findByUserId(String userId) {
        log.info("Fetching user memory for userId: {}", userId);
        
        return Mono.fromCallable(() -> {
            try {
                DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(userId);
                DocumentSnapshot snapshot = docRef.get().get();
                
                if (snapshot.exists()) {
                    return snapshot.toObject(UserMemory.class);
                } else {
                    log.info("No user memory found for userId: {}", userId);
                    return null;
                }
            } catch (ExecutionException e) {
                // Firebase yetkilendirme hatası kontrolü
                if (e.getCause() instanceof PermissionDeniedException) {
                    log.error("Firebase permission denied: {}", e.getCause().getMessage());
                    throw new FirebaseAuthException(
                            "Firebase yetkilendirme hatası: Servis hesabının gerekli izinleri yok", e);
                }
                
                log.error("Error fetching user memory: {}", e.getMessage(), e);
                throw new RuntimeException("Error fetching user memory", e);
            } catch (Exception e) {
                log.error("Unexpected error fetching user memory: {}", e.getMessage(), e);
                throw new RuntimeException("Error accessing user memory data", e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<UserMemory> save(UserMemory userMemory) {
        return Mono.fromCallable(() -> {
            try {
                DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(userMemory.getUserId());
                docRef.set(userMemory).get();
                log.info("User memory saved successfully for userId: {}", userMemory.getUserId());
                return userMemory;
            } catch (ExecutionException e) {
                if (e.getCause() instanceof PermissionDeniedException) {
                    log.error("Firebase permission denied during save: {}", e.getCause().getMessage());
                    throw new FirebaseAuthException(
                            "Firebase yetkilendirme hatası: Servis hesabının yazma izni yok", e);
                }
                
                log.error("Error saving user memory: {}", e.getMessage(), e);
                throw new RuntimeException("Error saving user memory", e);
            } catch (Exception e) {
                log.error("Unexpected error saving user memory: {}", e.getMessage(), e);
                throw new RuntimeException("Error saving user memory data", e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
