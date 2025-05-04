package com.craftpilot.usermemoryservice.repository;

import com.craftpilot.usermemoryservice.model.UserMemory;
import com.google.cloud.firestore.Firestore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Repository
@RequiredArgsConstructor
@Slf4j
public class UserMemoryRepository {
    private final Firestore firestore;
    private static final String COLLECTION_NAME = "userMemory";

    public Mono<UserMemory> findByUserId(String userId) {
        return Mono.fromCallable(() -> 
            firestore.collection(COLLECTION_NAME)
                    .document(userId)
                    .get()
                    .get()
                    .toObject(UserMemory.class)
        ).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<UserMemory> save(UserMemory userMemory) {
        return Mono.fromCallable(() -> {
            firestore.collection(COLLECTION_NAME)
                    .document(userMemory.getUserId())
                    .set(userMemory)
                    .get();
            return userMemory;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Void> deleteByUserId(String userId) {
        return Mono.fromRunnable(() -> {
            try {
                firestore.collection(COLLECTION_NAME)
                       .document(userId)
                       .delete()
                       .get();
            } catch (Exception e) {
                throw new RuntimeException("Error deleting user memory", e);
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }
}
