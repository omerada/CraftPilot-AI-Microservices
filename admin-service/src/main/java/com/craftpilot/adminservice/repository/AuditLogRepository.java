package com.craftpilot.adminservice.repository;

import com.craftpilot.adminservice.model.AuditLog;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

@Repository
public interface AuditLogRepository extends ReactiveMongoRepository<AuditLog, String> {
    
    Flux<AuditLog> findByUserId(String userId);
    
    Flux<AuditLog> findByServiceId(String serviceId);
    
    Flux<AuditLog> findByLogType(AuditLog.LogType logType);
    
    Flux<AuditLog> findByStatus(AuditLog.LogStatus status);
    
    Flux<AuditLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
    
    Flux<AuditLog> findByResource(String resource);
    
    Flux<AuditLog> findByResourceAndLogType(String resource, AuditLog.LogType logType);
    
    Flux<AuditLog> findByUserIdAndTimestampBetween(String userId, LocalDateTime start, LocalDateTime end);
    
    Flux<AuditLog> findByUserIdAndLogType(String userId, AuditLog.LogType logType);
}