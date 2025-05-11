package com.craftpilot.adminservice.repository;

import com.craftpilot.adminservice.model.SystemMetrics;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
public interface SystemMetricsRepository extends ReactiveMongoRepository<SystemMetrics, String> {
    
    Mono<SystemMetrics> findByServiceId(String serviceId);
    
    Flux<SystemMetrics> findByServiceType(SystemMetrics.ServiceType serviceType);
    
    Flux<SystemMetrics> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
    
    Flux<SystemMetrics> findByStatus(SystemMetrics.ServiceStatus status);
    
    Flux<SystemMetrics> findByServiceIdAndTimestampBetween(String serviceId, LocalDateTime start, LocalDateTime end);
    
    Flux<SystemMetrics> findByServiceTypeAndStatus(SystemMetrics.ServiceType serviceType, SystemMetrics.ServiceStatus status);
}