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
    
    @Query("{ 'timestamp' : { $gte : ?0, $lte : ?1 } }")
    Flux<PerformanceMetrics> findByTimeRange(LocalDateTime start, LocalDateTime end);
    
    Flux<PerformanceMetrics> findByModelIdAndType(String modelId, PerformanceMetrics.MetricType type);
    
    @Query("{ 'timestamp' : { $gte : ?0 }, 'serviceId' : ?1 }")
    Flux<PerformanceMetrics> findByTimestampAfterAndServiceId(LocalDateTime timestamp, String serviceId);
}