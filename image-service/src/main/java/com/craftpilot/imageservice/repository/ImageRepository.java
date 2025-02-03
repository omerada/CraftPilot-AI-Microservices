package com.craftpilot.imageservice.repository;

import com.craftpilot.imageservice.model.Image;
import com.google.cloud.firestore.Firestore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Repository
@RequiredArgsConstructor
public class ImageRepository {
    private static final String COLLECTION_NAME = "images";
    private final Firestore firestore;

    public Mono<Image> save(Image image) {
        if (image.getId() == null) {
            image.setId(firestore.collection(COLLECTION_NAME).document().getId());
        }
        if (image.getCreatedAt() == null) {
            image.setCreatedAt(LocalDateTime.now());
        }
        image.setUpdatedAt(LocalDateTime.now());

        return Mono.fromFuture(() -> CompletableFuture.supplyAsync(() -> {
            try {
                firestore.collection(COLLECTION_NAME)
                        .document(image.getId())
                        .set(image)
                        .get();
                return image;
            } catch (Exception e) {
                throw new RuntimeException("Error saving image", e);
            }
        }));
    }

    public Mono<Image> findById(String id) {
        return Mono.fromFuture(() -> CompletableFuture.supplyAsync(() -> {
            try {
                var documentSnapshot = firestore.collection(COLLECTION_NAME)
                        .document(id)
                        .get()
                        .get();
                return documentSnapshot.toObject(Image.class);
            } catch (Exception e) {
                throw new RuntimeException("Error finding image by id", e);
            }
        }));
    }

    public Flux<Image> findByUserId(String userId) {
        return Mono.fromFuture(() -> CompletableFuture.supplyAsync(() -> {
            try {
                var querySnapshot = firestore.collection(COLLECTION_NAME)
                        .whereEqualTo("userId", userId)
                        .orderBy("createdAt")
                        .get()
                        .get();
                return querySnapshot.toObjects(Image.class);
            } catch (Exception e) {
                throw new RuntimeException("Error finding images by user id", e);
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
                throw new RuntimeException("Error deleting image", e);
            }
        })).then();
    }
} 