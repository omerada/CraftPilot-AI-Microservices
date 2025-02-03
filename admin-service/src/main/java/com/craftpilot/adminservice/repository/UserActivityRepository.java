package com.craftpilot.adminservice.repository;

import com.craftpilot.adminservice.model.UserActivity;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Repository
public class UserActivityRepository {
    private final Firestore firestore;
    private static final String COLLECTION_NAME = "user_activities";

    public UserActivityRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    public Mono<UserActivity> save(UserActivity activity) {
        if (activity.getId() == null) {
            activity.setId(firestore.collection(COLLECTION_NAME).document().getId());
        }
        ApiFuture<WriteResult> future = firestore.collection(COLLECTION_NAME)
                .document(activity.getId())
                .set(activity);
        return Mono.fromFuture(toCompletableFuture(future))
                .thenReturn(activity);
    }

    public Mono<UserActivity> findById(String id) {
        ApiFuture<DocumentSnapshot> future = firestore.collection(COLLECTION_NAME)
                .document(id)
                .get();
        return Mono.fromFuture(toCompletableFuture(future))
                .map(doc -> doc.toObject(UserActivity.class));
    }

    public Flux<UserActivity> findByUserId(String userId) {
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .get();
        return Mono.fromFuture(toCompletableFuture(future))
                .flatMapMany(querySnapshot -> Flux.fromIterable(querySnapshot.getDocuments()))
                .map(doc -> doc.toObject(UserActivity.class));
    }

    public Flux<UserActivity> findByActivityType(UserActivity.ActivityType activityType) {
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("activityType", activityType)
                .get();
        return Mono.fromFuture(toCompletableFuture(future))
                .flatMapMany(querySnapshot -> Flux.fromIterable(querySnapshot.getDocuments()))
                .map(doc -> doc.toObject(UserActivity.class));
    }

    public Flux<UserActivity> findByTimeRange(LocalDateTime start, LocalDateTime end) {
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME)
                .whereGreaterThanOrEqualTo("timestamp", start)
                .whereLessThanOrEqualTo("timestamp", end)
                .get();
        return Mono.fromFuture(toCompletableFuture(future))
                .flatMapMany(querySnapshot -> Flux.fromIterable(querySnapshot.getDocuments()))
                .map(doc -> doc.toObject(UserActivity.class));
    }

    public Flux<UserActivity> findByStatus(UserActivity.ActivityStatus status) {
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("status", status)
                .get();
        return Mono.fromFuture(toCompletableFuture(future))
                .flatMapMany(querySnapshot -> Flux.fromIterable(querySnapshot.getDocuments()))
                .map(doc -> doc.toObject(UserActivity.class));
    }

    public Mono<Void> deleteById(String id) {
        ApiFuture<WriteResult> future = firestore.collection(COLLECTION_NAME)
                .document(id)
                .delete();
        return Mono.fromFuture(toCompletableFuture(future))
                .then();
    }

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
} 