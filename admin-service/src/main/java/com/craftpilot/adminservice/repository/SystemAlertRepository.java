package com.craftpilot.adminservice.repository;

import com.craftpilot.adminservice.model.SystemAlert;
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
public class SystemAlertRepository {
    private final Firestore firestore;
    private static final String COLLECTION_NAME = "system_alerts";

    public SystemAlertRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    public Mono<SystemAlert> save(SystemAlert alert) {
        if (alert.getId() == null) {
            alert.setId(firestore.collection(COLLECTION_NAME).document().getId());
        }
        ApiFuture<WriteResult> future = firestore.collection(COLLECTION_NAME)
                .document(alert.getId())
                .set(alert);
        return Mono.fromFuture(toCompletableFuture(future))
                .thenReturn(alert);
    }

    public Mono<SystemAlert> findById(String id) {
        ApiFuture<DocumentSnapshot> future = firestore.collection(COLLECTION_NAME)
                .document(id)
                .get();
        return Mono.fromFuture(toCompletableFuture(future))
                .map(doc -> doc.toObject(SystemAlert.class));
    }

    public Flux<SystemAlert> findByServiceId(String serviceId) {
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("serviceId", serviceId)
                .get();
        return Mono.fromFuture(toCompletableFuture(future))
                .flatMapMany(querySnapshot -> Flux.fromIterable(querySnapshot.getDocuments()))
                .map(doc -> doc.toObject(SystemAlert.class));
    }

    public Flux<SystemAlert> findByAlertType(SystemAlert.AlertType alertType) {
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("alertType", alertType)
                .get();
        return Mono.fromFuture(toCompletableFuture(future))
                .flatMapMany(querySnapshot -> Flux.fromIterable(querySnapshot.getDocuments()))
                .map(doc -> doc.toObject(SystemAlert.class));
    }

    public Flux<SystemAlert> findBySeverity(SystemAlert.AlertSeverity severity) {
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("severity", severity)
                .get();
        return Mono.fromFuture(toCompletableFuture(future))
                .flatMapMany(querySnapshot -> Flux.fromIterable(querySnapshot.getDocuments()))
                .map(doc -> doc.toObject(SystemAlert.class));
    }

    public Flux<SystemAlert> findByStatus(SystemAlert.AlertStatus status) {
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("status", status)
                .get();
        return Mono.fromFuture(toCompletableFuture(future))
                .flatMapMany(querySnapshot -> Flux.fromIterable(querySnapshot.getDocuments()))
                .map(doc -> doc.toObject(SystemAlert.class));
    }

    public Flux<SystemAlert> findByTimeRange(LocalDateTime start, LocalDateTime end) {
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME)
                .whereGreaterThanOrEqualTo("timestamp", start)
                .whereLessThanOrEqualTo("timestamp", end)
                .get();
        return Mono.fromFuture(toCompletableFuture(future))
                .flatMapMany(querySnapshot -> Flux.fromIterable(querySnapshot.getDocuments()))
                .map(doc -> doc.toObject(SystemAlert.class));
    }

    public Flux<SystemAlert> findByAssignedTo(String assignedTo) {
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("assignedTo", assignedTo)
                .get();
        return Mono.fromFuture(toCompletableFuture(future))
                .flatMapMany(querySnapshot -> Flux.fromIterable(querySnapshot.getDocuments()))
                .map(doc -> doc.toObject(SystemAlert.class));
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