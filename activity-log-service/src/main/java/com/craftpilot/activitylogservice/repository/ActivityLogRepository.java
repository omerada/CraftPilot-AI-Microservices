package com.craftpilot.activitylogservice.repository;

import com.craftpilot.activitylogservice.model.ActivityLog;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Repository
@Slf4j
public class ActivityLogRepository {

    private static final String COLLECTION_NAME = "activity_logs";
    private final Firestore firestore;

    public ActivityLogRepository(Firestore firestore) {
        this.firestore = firestore;
    }
    
    public CollectionReference getCollection() {
        return firestore.collection(COLLECTION_NAME);
    }

    public Mono<ActivityLog> save(ActivityLog activityLog) {
        log.debug("Saving activity log: {}", activityLog);
        
        ApiFuture<WriteResult> future = getCollection()
                .document(activityLog.getId())
                .set(activityLog);
        
        return Mono.fromFuture(toCompletableFuture(future))
                .thenReturn(activityLog)
                .doOnSuccess(saved -> log.debug("Successfully saved activity log with ID: {}", saved.getId()))
                .doOnError(error -> log.error("Failed to save activity log: {}", error.getMessage()));
    }

    public Mono<ActivityLog> findById(String id) {
        ApiFuture<DocumentSnapshot> future = getCollection()
                .document(id)
                .get();
                
        return Mono.fromFuture(toCompletableFuture(future))
                .map(document -> document.exists() 
                    ? document.toObject(ActivityLog.class) 
                    : null);
    }

    public Flux<ActivityLog> findByUserId(String userId) {
        ApiFuture<QuerySnapshot> future = getCollection()
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get();
                
        return Mono.fromFuture(toCompletableFuture(future))
                .flatMapMany(querySnapshot -> Flux.fromIterable(querySnapshot.getDocuments()))
                .map(document -> document.toObject(ActivityLog.class));
    }

    public Flux<ActivityLog> findByActionType(String actionType) {
        ApiFuture<QuerySnapshot> future = getCollection()
                .whereEqualTo("actionType", actionType)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get();
                
        return Mono.fromFuture(toCompletableFuture(future))
                .flatMapMany(querySnapshot -> Flux.fromIterable(querySnapshot.getDocuments()))
                .map(document -> document.toObject(ActivityLog.class));
    }

    public Flux<ActivityLog> findByTimeRange(LocalDateTime start, LocalDateTime end) {
        Query query = getCollection().orderBy("timestamp", Query.Direction.DESCENDING);
        
        if (start != null) {
            query = query.whereGreaterThanOrEqualTo("timestamp", start);
        }
        
        if (end != null) {
            query = query.whereLessThanOrEqualTo("timestamp", end);
        }
        
        ApiFuture<QuerySnapshot> future = query.get();
                
        return Mono.fromFuture(toCompletableFuture(future))
                .flatMapMany(querySnapshot -> Flux.fromIterable(querySnapshot.getDocuments()))
                .map(document -> document.toObject(ActivityLog.class));
    }
    
    public Flux<ActivityLog> findByFilters(
            String userId, 
            String actionType, 
            LocalDateTime fromDate, 
            LocalDateTime toDate, 
            int limit, 
            String lastDocumentId) {
                
        Query query = getCollection().orderBy("timestamp", Query.Direction.DESCENDING);
        
        if (userId != null && !userId.isEmpty()) {
            query = query.whereEqualTo("userId", userId);
        }
        
        if (actionType != null && !actionType.isEmpty()) {
            query = query.whereEqualTo("actionType", actionType);
        }
        
        if (fromDate != null) {
            query = query.whereGreaterThanOrEqualTo("timestamp", fromDate);
        }
        
        if (toDate != null) {
            query = query.whereLessThanOrEqualTo("timestamp", toDate);
        }
        
        if (lastDocumentId != null && !lastDocumentId.isEmpty()) {
            // Create a final copy of query for use in the lambda
            final Query finalQuery = query;
            return findById(lastDocumentId)
                    .flatMapMany(lastDocument -> {
                        ApiFuture<QuerySnapshot> future = finalQuery
                                .startAfter(lastDocument)
                                .limit(limit)
                                .get();
                        
                        return Mono.fromFuture(toCompletableFuture(future))
                                .flatMapMany(querySnapshot -> Flux.fromIterable(querySnapshot.getDocuments()))
                                .map(document -> document.toObject(ActivityLog.class));
                    })
                    .switchIfEmpty(Flux.empty());
        }
        
        ApiFuture<QuerySnapshot> future = query
                .limit(limit)
                .get();
        
        return Mono.fromFuture(toCompletableFuture(future))
                .flatMapMany(querySnapshot -> Flux.fromIterable(querySnapshot.getDocuments()))
                .map(document -> document.toObject(ActivityLog.class));
    }
    
    public Mono<Long> countByFilters(String userId, String actionType, LocalDateTime fromDate, LocalDateTime toDate) {
        Query query = getCollection();
        
        if (userId != null && !userId.isEmpty()) {
            query = query.whereEqualTo("userId", userId);
        }
        
        if (actionType != null && !actionType.isEmpty()) {
            query = query.whereEqualTo("actionType", actionType);
        }
        
        if (fromDate != null) {
            query = query.whereGreaterThanOrEqualTo("timestamp", fromDate);
        }
        
        if (toDate != null) {
            query = query.whereLessThanOrEqualTo("timestamp", toDate);
        }
        
        ApiFuture<QuerySnapshot> future = query.get();
        
        return Mono.fromFuture(toCompletableFuture(future))
                .map(QuerySnapshot::size)
                .map(Integer::longValue);
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
