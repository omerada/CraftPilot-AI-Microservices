package com.craftpilot.analyticsservice.repository;

import com.craftpilot.analyticsservice.model.PerformanceMetrics;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

@Repository
public interface PerformanceMetricsRepository extends ReactiveMongoRepository<PerformanceMetrics, String> {
    
    Flux<PerformanceMetrics> findByModelId(String modelId);
    
    Flux<PerformanceMetrics> findByServiceId(String serviceId);
    
    Flux<PerformanceMetrics> findByType(PerformanceMetrics.MetricType type);
    
    @Query("{ 'timestamp': { $gte: ?0, $lte: ?1 } }")
    Flux<PerformanceMetrics> findByTimeRange(LocalDateTime start, LocalDateTime end);
    
    @Query("{ 'timestamp': { $gte: ?0 } }")
    Flux<PerformanceMetrics> findByTimestampAfter(LocalDateTime timestamp);
    
    @Query("{ 'timestamp': { $lte: ?0 } }")
    Flux<PerformanceMetrics> findByTimestampBefore(LocalDateTime timestamp);
    
    @Query("{ 'modelId': ?0, 'type': ?1 }")
    Flux<PerformanceMetrics> findByModelIdAndType(String modelId, PerformanceMetrics.MetricType type);
    
    @Query("{ 'serviceId': ?0, 'type': ?1 }")
    Flux<PerformanceMetrics> findByServiceIdAndType(String serviceId, PerformanceMetrics.MetricType type);
    
    @Query("{ 'createdAt': { $gte: ?0, $lte: ?1 } }")
    Flux<PerformanceMetrics> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}