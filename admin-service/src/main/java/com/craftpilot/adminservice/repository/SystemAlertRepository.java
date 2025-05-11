package com.craftpilot.adminservice.repository;

import com.craftpilot.adminservice.model.SystemAlert;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

@Repository
public interface SystemAlertRepository extends ReactiveMongoRepository<SystemAlert, String> {
    
    Flux<SystemAlert> findByServiceId(String serviceId);
    
    Flux<SystemAlert> findByAlertType(SystemAlert.AlertType alertType);
    
    Flux<SystemAlert> findBySeverity(SystemAlert.AlertSeverity severity);
    
    Flux<SystemAlert> findByStatus(SystemAlert.AlertStatus status);
    
    Flux<SystemAlert> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
    
    Flux<SystemAlert> findByAssignedTo(String assignedTo);
    
    Flux<SystemAlert> findByServiceIdAndStatusNot(String serviceId, SystemAlert.AlertStatus status);
    
    Flux<SystemAlert> findByStatusAndSeverityIn(SystemAlert.AlertStatus status, Iterable<SystemAlert.AlertSeverity> severities);
    
    Flux<SystemAlert> findByAffectedServicesContaining(String serviceId);
}