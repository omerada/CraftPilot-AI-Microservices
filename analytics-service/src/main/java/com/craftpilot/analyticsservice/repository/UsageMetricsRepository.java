package com.craftpilot.analyticsservice.repository;

import com.craftpilot.analyticsservice.model.UsageMetrics;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

@Repository
public interface UsageMetricsRepository extends ReactiveMongoRepository<UsageMetrics, String> {
    
    Flux<UsageMetrics> findByUserId(String userId);
    
    Flux<UsageMetrics> findByServiceType(UsageMetrics.ServiceType serviceType);
    
    @Query("{ 'startTime' : { $gte : ?0 }, 'endTime' : { $lte : ?1 } }")
    Flux<UsageMetrics> findByTimeRange(LocalDateTime start, LocalDateTime end);
    
    Flux<UsageMetrics> findByModelId(String modelId);
    
    Flux<UsageMetrics> findByUserIdAndServiceType(String userId, UsageMetrics.ServiceType serviceType);
    
    @Query("{ 'startTime' : { $gte : ?0 }, 'userId' : ?1 }")
    Flux<UsageMetrics> findByStartTimeAfterAndUserId(LocalDateTime startTime, String userId);
}