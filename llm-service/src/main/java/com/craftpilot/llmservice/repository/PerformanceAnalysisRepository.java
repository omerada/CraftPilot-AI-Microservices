package com.craftpilot.llmservice.repository;

import com.craftpilot.llmservice.model.performance.PerformanceAnalysisResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
public interface PerformanceAnalysisRepository extends ReactiveMongoRepository<PerformanceAnalysisResponse, String> {

    Flux<PerformanceAnalysisResponse> findByUrl(String url);

    Flux<PerformanceAnalysisResponse> findByModelId(String modelId);

    Flux<PerformanceAnalysisResponse> findBySessionId(String sessionId);

    @Query("{ 'status': ?0 }")
    Flux<PerformanceAnalysisResponse> findByStatus(String status);

    @Query("{ 'timestamp': { $gte: ?0, $lte: ?1 } }")
    Flux<PerformanceAnalysisResponse> findByTimeRange(LocalDateTime start, LocalDateTime end);

    @Query("{ 'modelId': ?0, 'timestamp': { $gte: ?1, $lte: ?2 } }")
    Flux<PerformanceAnalysisResponse> findByModelIdAndTimeRange(String modelId, LocalDateTime start, LocalDateTime end);

    @Query("{ 'sessionId': ?0, 'timestamp': { $gte: ?1, $lte: ?2 } }")
    Flux<PerformanceAnalysisResponse> findBySessionIdAndTimeRange(String sessionId, LocalDateTime start,
            LocalDateTime end);

    @Query(value = "{ 'modelId': ?0 }", sort = "{ 'timestamp': -1 }")
    Flux<PerformanceAnalysisResponse> findByModelIdOrderByTimestampDesc(String modelId, Pageable pageable);

    @Aggregation(pipeline = {
            "{ $match: { 'modelId': ?0 } }",
            "{ $group: { '_id': null, 'avgProcessingTimeMs': { $avg: '$processingTimeMs' }, 'totalRequests': { $sum: 1 } } }"
    })
    Mono<ModelPerformanceStats> getModelPerformanceStats(String modelId);

    public static interface ModelPerformanceStats {
        Double getAvgProcessingTimeMs();

        Integer getTotalRequests();
    }
}
