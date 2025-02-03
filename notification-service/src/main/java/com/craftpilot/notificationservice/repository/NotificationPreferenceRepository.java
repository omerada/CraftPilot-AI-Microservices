package com.craftpilot.notificationservice.repository;

import com.craftpilot.notificationservice.model.NotificationPreference;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

@Repository
@RequiredArgsConstructor
public class NotificationPreferenceRepository {

    private final Firestore firestore;
    private static final String COLLECTION_NAME = "notification_preferences";

    public Mono<NotificationPreference> save(NotificationPreference preference) {
        if (preference.getId() == null) {
            preference.setId(firestore.collection(COLLECTION_NAME).document().getId());
        }
        ApiFuture<WriteResult> future = firestore.collection(COLLECTION_NAME)
                .document(preference.getId())
                .set(preference);
                
        return Mono.fromFuture(() -> CompletableFuture.supplyAsync(() -> {
            try {
                future.get();
                return preference;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }));
    }

    public Mono<NotificationPreference> findById(String id) {
        ApiFuture<DocumentSnapshot> future = firestore.collection(COLLECTION_NAME)
                .document(id)
                .get();
                
        return Mono.fromFuture(() -> CompletableFuture.supplyAsync(() -> {
            try {
                DocumentSnapshot document = future.get();
                return document.exists() ? document.toObject(NotificationPreference.class) : null;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }));
    }

    public Mono<NotificationPreference> findByUserId(String userId) {
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .whereEqualTo("deleted", false)
                .get();
                
        return Mono.fromFuture(() -> CompletableFuture.supplyAsync(() -> {
            try {
                QuerySnapshot querySnapshot = future.get();
                if (querySnapshot.isEmpty()) {
                    return null;
                }
                return querySnapshot.getDocuments().get(0).toObject(NotificationPreference.class);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }));
    }

    public Mono<Void> deleteByUserId(String userId) {
        return findByUserId(userId)
                .flatMap(preference -> {
                    ApiFuture<WriteResult> future = firestore.collection(COLLECTION_NAME)
                            .document(preference.getId())
                            .update("deleted", true);
                            
                    return Mono.fromFuture(() -> CompletableFuture.supplyAsync(() -> {
                        try {
                            future.get();
                            return null;
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }));
                });
    }
} 