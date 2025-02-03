package com.craftpilot.notificationservice.repository;

import com.craftpilot.notificationservice.model.NotificationTemplate;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

@Repository
@RequiredArgsConstructor
public class NotificationTemplateRepository {

    private final Firestore firestore;
    private static final String COLLECTION_NAME = "notification_templates";

    public Mono<NotificationTemplate> save(NotificationTemplate template) {
        if (template.getId() == null) {
            template.setId(firestore.collection(COLLECTION_NAME).document().getId());
        }
        ApiFuture<WriteResult> future = firestore.collection(COLLECTION_NAME)
                .document(template.getId())
                .set(template);
                
        return Mono.fromFuture(() -> CompletableFuture.supplyAsync(() -> {
            try {
                future.get();
                return template;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }));
    }

    public Mono<NotificationTemplate> findById(String id) {
        ApiFuture<DocumentSnapshot> future = firestore.collection(COLLECTION_NAME)
                .document(id)
                .get();
                
        return Mono.fromFuture(() -> CompletableFuture.supplyAsync(() -> {
            try {
                DocumentSnapshot document = future.get();
                return document.exists() ? document.toObject(NotificationTemplate.class) : null;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }));
    }

    public Flux<NotificationTemplate> findByActiveAndDeleted(boolean active, boolean deleted) {
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("active", active)
                .whereEqualTo("deleted", deleted)
                .get();
                
        return Mono.fromFuture(() -> CompletableFuture.supplyAsync(() -> {
            try {
                return future.get().getDocuments().stream()
                    .map(doc -> doc.toObject(NotificationTemplate.class))
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