package com.craftpilot.analyticsservice.repository;

import com.craftpilot.analyticsservice.model.AnalyticsReport;
import com.google.cloud.firestore.Firestore;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
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
public class AnalyticsReportRepository {
    private static final String COLLECTION_NAME = "analytics_reports";
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

    public Mono<AnalyticsReport> save(AnalyticsReport report) {
        if (report.getId() == null) {
            report.setId(firestore.collection(COLLECTION_NAME).document().getId());
            report.setCreatedAt(LocalDateTime.now());
        }
        report.setUpdatedAt(LocalDateTime.now());

        ApiFuture<WriteResult> future = firestore.collection(COLLECTION_NAME)
                .document(report.getId())
                .set(report);

        return Mono.fromFuture(toCompletableFuture(future))
                .thenReturn(report);
    }

    public Mono<AnalyticsReport> findById(String id) {
        ApiFuture<DocumentSnapshot> future = firestore.collection(COLLECTION_NAME)
                .document(id)
                .get();

        return Mono.fromFuture(toCompletableFuture(future))
                .map(documentSnapshot -> documentSnapshot.toObject(AnalyticsReport.class));
    }

    public Flux<AnalyticsReport> findByType(AnalyticsReport.ReportType type) {
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("type", type)
                .get();

        return Mono.fromFuture(toCompletableFuture(future))
                .flatMapMany(querySnapshot -> Flux.fromIterable(querySnapshot.toObjects(AnalyticsReport.class)));
    }

    public Flux<AnalyticsReport> findByStatus(AnalyticsReport.ReportStatus status) {
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("status", status)
                .get();

        return Mono.fromFuture(toCompletableFuture(future))
                .flatMapMany(querySnapshot -> Flux.fromIterable(querySnapshot.toObjects(AnalyticsReport.class)));
    }

    public Flux<AnalyticsReport> findByCreatedBy(String userId) {
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("createdBy", userId)
                .get();

        return Mono.fromFuture(toCompletableFuture(future))
                .flatMapMany(querySnapshot -> Flux.fromIterable(querySnapshot.toObjects(AnalyticsReport.class)));
    }

    public Flux<AnalyticsReport> findByTimeRange(LocalDateTime start, LocalDateTime end) {
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME)
                .whereGreaterThanOrEqualTo("reportStartTime", start)
                .whereLessThanOrEqualTo("reportEndTime", end)
                .get();

        return Mono.fromFuture(toCompletableFuture(future))
                .flatMapMany(querySnapshot -> Flux.fromIterable(querySnapshot.toObjects(AnalyticsReport.class)));
    }

    public Mono<Void> deleteById(String id) {
        ApiFuture<WriteResult> future = firestore.collection(COLLECTION_NAME)
                .document(id)
                .delete();

        return Mono.fromFuture(toCompletableFuture(future))
                .then();
    }
} 