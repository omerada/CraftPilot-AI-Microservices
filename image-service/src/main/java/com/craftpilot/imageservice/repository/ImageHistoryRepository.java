package com.craftpilot.imageservice.repository;

import com.craftpilot.imageservice.model.ImageHistory;
import com.google.cloud.firestore.Firestore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Repository
@RequiredArgsConstructor
public class ImageHistoryRepository {
    private static final String COLLECTION_NAME = "image_histories";
    private final Firestore firestore;

    public Mono<ImageHistory> save(ImageHistory imageHistory) {
        if (imageHistory.getId() == null) {
            imageHistory.setId(firestore.collection(COLLECTION_NAME).document().getId());
        }
        if (imageHistory.getCreatedAt() == null) {
            imageHistory.setCreatedAt(LocalDateTime.now());
        }
        imageHistory.setUpdatedAt(LocalDateTime.now());

        return Mono.fromFuture(() -> CompletableFuture.supplyAsync(() -> {
            try {
                firestore.collection(COLLECTION_NAME)
                        .document(imageHistory.getId())
                        .set(imageHistory)
                        .get();
                return imageHistory;
            } catch (Exception e) {
                throw new RuntimeException("Error saving image history", e);
            }
        }));
    }

    public Mono<ImageHistory> findById(String id) {
        return Mono.fromFuture(() -> CompletableFuture.supplyAsync(() -> {
            try {
                var documentSnapshot = firestore.collection(COLLECTION_NAME)
                        .document(id)
                        .get()
                        .get();
                return documentSnapshot.toObject(ImageHistory.class);
            } catch (Exception e) {
                throw new RuntimeException("Error finding image history by id", e);
            }
        }));
    }

    public Flux<ImageHistory> findByUserId(String userId) {
        return Mono.fromFuture(() -> CompletableFuture.supplyAsync(() -> {
            try {
                var querySnapshot = firestore.collection(COLLECTION_NAME)
                        .whereEqualTo("userId", userId)
                        .orderBy("createdAt")
                        .get()
                        .get();
                return querySnapshot.toObjects(ImageHistory.class);
            } catch (Exception e) {
                throw new RuntimeException("Error finding image histories by user id", e);
            }
        })).flatMapMany(Flux::fromIterable);
    }

    public Mono<Void> deleteById(String id) {
        return Mono.fromFuture(() -> CompletableFuture.supplyAsync(() -> {
            try {
                firestore.collection(COLLECTION_NAME)
                        .document(id)
                        .delete()
                        .get();
                return null;
            } catch (Exception e) {
                throw new RuntimeException("Error deleting image history", e);
            }
        })).then();
    }
} 