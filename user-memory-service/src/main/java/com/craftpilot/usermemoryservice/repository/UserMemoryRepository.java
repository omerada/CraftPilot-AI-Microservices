package com.craftpilot.usermemoryservice.repository;

import com.craftpilot.usermemoryservice.model.UserMemory;
import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Repository
@RequiredArgsConstructor
@Slf4j
public class UserMemoryRepository {

    private final Firestore firestore;
    
    @Value("${firestore.collection.user-memory}")
    private String collectionName;

    public Mono<UserMemory> findByUserId(String userId) {
        return Mono.fromCallable(() -> {
            try {
                log.info("Fetching user memory for userId: {}", userId);
                DocumentSnapshot documentSnapshot = firestore.collection(collectionName)
                        .document(userId)
                        .get()
                        .get();
                
                if (documentSnapshot.exists()) {
                    UserMemory userMemory = documentSnapshot.toObject(UserMemory.class);
                    log.info("Found user memory with {} entries", 
                            userMemory.getEntries() != null ? userMemory.getEntries().size() : 0);
                    return userMemory;
                }
                log.info("No user memory found for userId: {}", userId);
                return null;
            } catch (InterruptedException | ExecutionException e) {
                log.error("Error fetching user memory: {}", e.getMessage(), e);
                throw new RuntimeException("Error fetching user memory", e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<UserMemory> save(UserMemory userMemory) {
        return Mono.fromCallable(() -> {
            try {
                log.info("Saving user memory for userId: {}", userMemory.getUserId());
                
                // Ensure timestamps are set
                Timestamp now = Timestamp.now();
                if (userMemory.getCreatedAt() == null) {
                    userMemory.setCreatedAt(now);
                }
                userMemory.setUpdatedAt(now);
                
                // Use userId as document ID
                String documentId = userMemory.getUserId();
                userMemory.setId(documentId);
                
                // Ensure each memory entry has an ID
                if (userMemory.getEntries() != null) {
                    userMemory.getEntries().forEach(entry -> {
                        if (entry.getId() == null) {
                            entry.setId(UUID.randomUUID().toString());
                        }
                        if (entry.getTimestamp() == null) {
                            entry.setTimestamp(now);
                        }
                    });
                }
                
                // Save to Firestore
                ApiFuture<WriteResult> writeResult = firestore.collection(collectionName)
                        .document(documentId)
                        .set(userMemory);
                
                writeResult.get(); // Wait for the write to complete
                log.info("User memory saved successfully for userId: {}", userMemory.getUserId());
                return userMemory;
            } catch (InterruptedException | ExecutionException e) {
                log.error("Error saving user memory: {}", e.getMessage(), e);
                throw new RuntimeException("Error saving user memory", e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
