package com.craftpilot.contentservice.repository;

import com.google.cloud.firestore.*;
import com.craftpilot.contentservice.model.Content;
import com.craftpilot.contentservice.model.ContentStatus;
import com.craftpilot.contentservice.model.ContentType;
import com.google.api.core.ApiFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Repository
@RequiredArgsConstructor
public class FirestoreContentRepository implements ContentRepository {

    private final Firestore firestore;
    private static final String COLLECTION_NAME = "contents";

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

    @Override
    public Mono<Content> save(Content content) {
        if (content.getId() == null) {
            content.setId(firestore.collection(COLLECTION_NAME).document().getId());
        }
        if (content.getCreatedAt() == null) {
            content.setCreatedAt(Instant.now());
        }
        content.setUpdatedAt(Instant.now());

        ApiFuture<WriteResult> future = firestore.collection(COLLECTION_NAME)
                .document(content.getId())
                .set(content);

        return Mono.fromFuture(toCompletableFuture(future))
                .thenReturn(content);
    }

    @Override
    public Mono<Content> findById(String id) {
        ApiFuture<DocumentSnapshot> future = firestore.collection(COLLECTION_NAME)
                .document(id)
                .get();

        return Mono.fromFuture(toCompletableFuture(future))
                .map(doc -> doc.exists() ? doc.toObject(Content.class) : null);
    }

    @Override
    public Flux<Content> findAll() {
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME)
                .orderBy("createdAt")
                .get();

        return Mono.fromFuture(toCompletableFuture(future))
                .flatMapMany(querySnapshot -> Flux.fromIterable(querySnapshot.toObjects(Content.class)));
    }

    @Override
    public Mono<Void> deleteById(String id) {
        ApiFuture<WriteResult> future = firestore.collection(COLLECTION_NAME)
                .document(id)
                .delete();

        return Mono.fromFuture(toCompletableFuture(future))
                .then();
    }

    @Override
    public Flux<Content> findByType(ContentType type) {
        return Mono.fromFuture(toCompletableFuture(
                firestore.collection(COLLECTION_NAME)
                        .whereEqualTo("type", type)
                        .get()))
                .flatMapMany(querySnapshot -> Flux.fromIterable(querySnapshot.getDocuments())
                        .map(doc -> doc.toObject(Content.class)));
    }

    @Override
    public Flux<Content> findByUserId(String userId) {
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .orderBy("createdAt")
                .get();

        return Mono.fromFuture(toCompletableFuture(future))
                .flatMapMany(querySnapshot -> Flux.fromIterable(querySnapshot.toObjects(Content.class)));
    }

    @Override
    public Flux<Content> findByStatus(ContentStatus status) {
        return Mono.fromFuture(toCompletableFuture(
                firestore.collection(COLLECTION_NAME)
                        .whereEqualTo("status", status)
                        .get()))
                .flatMapMany(querySnapshot -> Flux.fromIterable(querySnapshot.getDocuments())
                        .map(doc -> doc.toObject(Content.class)));
    }

    @Override
    public Flux<Content> findByTags(List<String> tags) {
        return Mono.fromFuture(toCompletableFuture(
                firestore.collection(COLLECTION_NAME)
                        .whereArrayContainsAny("tags", tags)
                        .get()))
                .flatMapMany(querySnapshot -> Flux.fromIterable(querySnapshot.getDocuments())
                        .map(doc -> doc.toObject(Content.class)));
    }

    @Override
    public Flux<Content> findByMetadata(Map<String, String> metadata) {
        Query query = firestore.collection(COLLECTION_NAME);
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            query = query.whereEqualTo("metadata." + entry.getKey(), entry.getValue());
        }
        return Mono.fromFuture(toCompletableFuture(query.get()))
                .flatMapMany(querySnapshot -> Flux.fromIterable(querySnapshot.getDocuments())
                        .map(doc -> doc.toObject(Content.class)));
    }

    @Override
    public Flux<Content> search(String query) {
        String lowercaseQuery = query.toLowerCase();
        return findAll()
                .filter(content ->
                        (content.getTitle() != null && content.getTitle().toLowerCase().contains(lowercaseQuery)) ||
                        (content.getDescription() != null && content.getDescription().toLowerCase().contains(lowercaseQuery)) ||
                        (content.getContent() != null && content.getContent().toLowerCase().contains(lowercaseQuery)) ||
                        (content.getTags() != null && content.getTags().stream()
                                .anyMatch(tag -> tag.toLowerCase().contains(lowercaseQuery)))
                );
    }
} 