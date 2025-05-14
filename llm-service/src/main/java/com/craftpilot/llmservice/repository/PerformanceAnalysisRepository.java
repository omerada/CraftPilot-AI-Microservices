package com.craftpilot.llmservice.repository;

import com.craftpilot.llmservice.model.performance.PerformanceAnalysisResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

@Repository
@Slf4j
public interface PerformanceAnalysisRepository extends ReactiveMongoRepository<PerformanceAnalysisResponse, String> {
    
    Flux<PerformanceAnalysisResponse> findByUrl(String url);

    Flux<PerformanceAnalysisResponse> findByModelId(String modelId);

    Flux<PerformanceAnalysisResponse> findBySessionId(String sessionId);

    @Query("{ 'timestamp': { $gte: ?0, $lte: ?1 } }")
    Flux<PerformanceAnalysisResponse> findByTimeRange(LocalDateTime start, LocalDateTime end);
}
