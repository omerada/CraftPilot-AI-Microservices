package com.craftpilot.contentservice.repository;

import com.craftpilot.contentservice.model.ChatHistory;
import com.google.cloud.firestore.Firestore;
import com.google.api.core.ApiFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Repository
@RequiredArgsConstructor
public class FirestoreChatHistoryRepository implements ChatHistoryRepository {
    private static final String COLLECTION_NAME = "chat_histories";
    private final Firestore firestore;

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
    public Mono<ChatHistory> save(ChatHistory chatHistory) {
        if (chatHistory.getId() == null) {
            chatHistory.setId(firestore.collection(COLLECTION_NAME).document().getId());
        }
        if (chatHistory.getCreatedAt() == null) {
            chatHistory.setCreatedAt(Instant.now());
        }
        chatHistory.setUpdatedAt(Instant.now());

        return Mono.fromFuture(toCompletableFuture(firestore.collection(COLLECTION_NAME)
                .document(chatHistory.getId())
                .set(chatHistory)))
                .doOnSuccess(result -> log.info("Chat history saved successfully: {}", chatHistory.getId()))
                .doOnError(error -> log.error("Error saving chat history: {}", error.getMessage()))
                .thenReturn(chatHistory);
    }

    @Override
    public Mono<ChatHistory> findById(String id) {
        return Mono.fromFuture(toCompletableFuture(firestore.collection(COLLECTION_NAME)
                .document(id)
                .get()))
                .map(documentSnapshot -> documentSnapshot.toObject(ChatHistory.class))
                .doOnSuccess(chatHistory -> log.info("Chat history retrieved successfully: {}", id))
                .doOnError(error -> log.error("Error retrieving chat history: {}", error.getMessage()));
    }

    @Override
    public Flux<ChatHistory> findByUserId(String userId) {
        return Mono.fromFuture(toCompletableFuture(firestore.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .orderBy("updatedAt")
                .limit(5)
                .get()))
                .flatMapMany(querySnapshot -> Flux.fromIterable(querySnapshot.toObjects(ChatHistory.class)))
                .doOnComplete(() -> log.info("Chat histories retrieved successfully for user: {}", userId))
                .doOnError(error -> log.error("Error retrieving chat histories for user: {}", error.getMessage()));
    }

    @Override
    public Flux<ChatHistory> findByContentId(String contentId) {
        return Mono.fromFuture(toCompletableFuture(firestore.collection(COLLECTION_NAME)
                .whereEqualTo("contentId", contentId)
                .orderBy("updatedAt")
                .get()))
                .flatMapMany(querySnapshot -> Flux.fromIterable(querySnapshot.toObjects(ChatHistory.class)))
                .doOnComplete(() -> log.info("Chat histories retrieved successfully for content: {}", contentId))
                .doOnError(error -> log.error("Error retrieving chat histories for content: {}", error.getMessage()));
    }

    @Override
    public Mono<Void> deleteById(String id) {
        return Mono.fromFuture(toCompletableFuture(firestore.collection(COLLECTION_NAME)
                .document(id)
                .delete()))
                .doOnSuccess(result -> log.info("Chat history deleted successfully: {}", id))
                .doOnError(error -> log.error("Error deleting chat history: {}", error.getMessage()))
                .then();
    }
} 