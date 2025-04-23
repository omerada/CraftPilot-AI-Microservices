package com.craftpilot.llmservice.repository;

import com.craftpilot.llmservice.model.performance.PerformanceAnalysisResponse;
import com.google.cloud.firestore.Firestore;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.WriteResult;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.DocumentSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

@Repository
@RequiredArgsConstructor
@Slf4j
public class PerformanceAnalysisRepository {
    private static final String COLLECTION_NAME = "performanceAnalysis";
    
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
    
    public Mono<PerformanceAnalysisResponse> save(PerformanceAnalysisResponse analysis) {
        ApiFuture<WriteResult> future = firestore.collection(COLLECTION_NAME)
                .document(analysis.getId())
                .set(analysis);

        return Mono.fromFuture(toCompletableFuture(future).thenApply(writeResult -> {
            log.info("Performance analysis saved with ID: {}", analysis.getId());
            return analysis;
        }));
    }
    
    public Flux<PerformanceAnalysisResponse> findByUrl(String url) {
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("url", url)
                .orderBy("timestamp", com.google.cloud.firestore.Query.Direction.DESCENDING)
                .limit(10)
                .get();

        return Mono.fromFuture(toCompletableFuture(future))
                .flatMapMany(querySnapshot -> Flux.fromIterable(querySnapshot.toObjects(PerformanceAnalysisResponse.class)));
    }
    
    public Mono<PerformanceAnalysisResponse> findById(String id) {
        ApiFuture<DocumentSnapshot> future = firestore.collection(COLLECTION_NAME)
                .document(id)
                .get();

        return Mono.fromFuture(toCompletableFuture(future)
                .thenApply(documentSnapshot -> documentSnapshot.toObject(PerformanceAnalysisResponse.class)));
    }
}
