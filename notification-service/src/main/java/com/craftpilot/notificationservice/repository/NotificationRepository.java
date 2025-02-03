package com.craftpilot.notificationservice.repository;

import com.craftpilot.notificationservice.model.Notification;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Repository
@RequiredArgsConstructor
public class NotificationRepository {

    private final Firestore firestore;
    private static final String COLLECTION_NAME = "notifications";

    public Mono<Notification> save(Notification notification) {
        if (notification.getId() == null) {
            notification.setId(firestore.collection(COLLECTION_NAME).document().getId());
        }
        ApiFuture<WriteResult> future = firestore.collection(COLLECTION_NAME)
                .document(notification.getId())
                .set(notification);
                
        return Mono.fromFuture(() -> CompletableFuture.supplyAsync(() -> {
            try {
                future.get();
                return notification;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }));
    }

    public Mono<Notification> findById(String id) {
        ApiFuture<DocumentSnapshot> future = firestore.collection(COLLECTION_NAME)
                .document(id)
                .get();
                
        return Mono.fromFuture(() -> CompletableFuture.supplyAsync(() -> {
            try {
                DocumentSnapshot document = future.get();
                return document.exists() ? document.toObject(Notification.class) : null;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }));
    }

    public Flux<Notification> findByUserId(String userId) {
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .whereEqualTo("deleted", false)
                .get();
                
        return Mono.fromFuture(() -> CompletableFuture.supplyAsync(() -> {
            try {
                return future.get().getDocuments().stream()
                    .map(doc -> doc.toObject(Notification.class))
                    .toList();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        })).flatMapMany(Flux::fromIterable);
    }

    public Flux<Notification> findByUserIdAndRead(String userId, boolean read) {
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .whereEqualTo("read", read)
                .whereEqualTo("deleted", false)
                .get();
                
        return Mono.fromFuture(() -> CompletableFuture.supplyAsync(() -> {
            try {
                return future.get().getDocuments().stream()
                    .map(doc -> doc.toObject(Notification.class))
                    .toList();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        })).flatMapMany(Flux::fromIterable);
    }

    public Flux<Notification> findByScheduledAtAfter(LocalDateTime dateTime) {
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME)
                .whereGreaterThan("scheduledAt", dateTime)
                .whereEqualTo("deleted", false)
                .get();
                
        return Mono.fromFuture(() -> CompletableFuture.supplyAsync(() -> {
            try {
                return future.get().getDocuments().stream()
                    .map(doc -> doc.toObject(Notification.class))
                    .toList();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        })).flatMapMany(Flux::fromIterable);
    }

    public Mono<Void> deleteById(String id) {
        ApiFuture<WriteResult> future = firestore.collection(COLLECTION_NAME)
                .document(id)
                .update("deleted", true);
                
        return Mono.fromFuture(() -> CompletableFuture.supplyAsync(() -> {
            try {
                future.get();
                return null;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }));
    }
} 