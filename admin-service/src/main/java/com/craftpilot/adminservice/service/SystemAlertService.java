package com.craftpilot.adminservice.service;

import com.craftpilot.adminservice.model.SystemAlert;
import com.craftpilot.adminservice.repository.SystemAlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemAlertService {
    private final SystemAlertRepository systemAlertRepository;

    public Mono<SystemAlert> createAlert(SystemAlert alert) {
        log.info("Creating system alert for service: {}", alert.getServiceId());
        return systemAlertRepository.save(alert);
    }

    public Mono<SystemAlert> getAlertById(String id) {
        log.info("Retrieving system alert with ID: {}", id);
        return systemAlertRepository.findById(id);
    }

    public Flux<SystemAlert> getAlertsByService(String serviceId) {
        log.info("Retrieving alerts for service: {}", serviceId);
        return systemAlertRepository.findByServiceId(serviceId);
    }

    public Flux<SystemAlert> getAlertsByType(SystemAlert.AlertType alertType) {
        log.info("Retrieving alerts of type: {}", alertType);
        return systemAlertRepository.findByAlertType(alertType);
    }

    public Flux<SystemAlert> getAlertsBySeverity(SystemAlert.AlertSeverity severity) {
        log.info("Retrieving alerts with severity: {}", severity);
        return systemAlertRepository.findBySeverity(severity);
    }

    public Flux<SystemAlert> getAlertsByStatus(SystemAlert.AlertStatus status) {
        log.info("Retrieving alerts with status: {}", status);
        return systemAlertRepository.findByStatus(status);
    }

    public Flux<SystemAlert> getAlertsByTimeRange(LocalDateTime start, LocalDateTime end) {
        log.info("Retrieving alerts between {} and {}", start, end);
        return systemAlertRepository.findByTimeRange(start, end);
    }

    public Flux<SystemAlert> getAlertsByAssignee(String assignedTo) {
        log.info("Retrieving alerts assigned to: {}", assignedTo);
        return systemAlertRepository.findByAssignedTo(assignedTo);
    }

    public Mono<Void> deleteAlert(String id) {
        log.info("Deleting alert with ID: {}", id);
        return systemAlertRepository.deleteById(id);
    }

    public Mono<SystemAlert> updateAlertStatus(String id, SystemAlert.AlertStatus newStatus) {
        log.info("Updating alert status for ID {} to {}", id, newStatus);
        return systemAlertRepository.findById(id)
                .flatMap(alert -> {
                    alert.setStatus(newStatus);
                    return systemAlertRepository.save(alert);
                });
    }

    public Mono<SystemAlert> assignAlert(String id, String assignedTo) {
        log.info("Assigning alert {} to {}", id, assignedTo);
        return systemAlertRepository.findById(id)
                .flatMap(alert -> {
                    alert.setAssignedTo(assignedTo);
                    alert.setStatus(SystemAlert.AlertStatus.IN_PROGRESS);
                    return systemAlertRepository.save(alert);
                });
    }

    public Mono<List<SystemAlert>> getActiveAlerts() {
        log.info("Retrieving active alerts");
        return systemAlertRepository.findByStatus(SystemAlert.AlertStatus.ACTIVE)
                .collectList();
    }

    public Mono<List<SystemAlert>> getCriticalAlerts() {
        log.info("Retrieving critical alerts");
        return systemAlertRepository.findBySeverity(SystemAlert.AlertSeverity.CRITICAL)
                .collectList();
    }

    public Mono<List<SystemAlert>> getUnassignedAlerts() {
        log.info("Retrieving unassigned alerts");
        return systemAlertRepository.findByAssignedTo(null)
                .collectList();
    }

    public Mono<SystemAlert> resolveAlert(String id, String resolution) {
        log.info("Resolving alert with ID: {}", id);
        return systemAlertRepository.findById(id)
                .flatMap(alert -> {
                    alert.setStatus(SystemAlert.AlertStatus.RESOLVED);
                    alert.setResolvedAt(LocalDateTime.now());
                    // Çözüm detaylarını metadata'ya ekle
                    alert.getMetadata().put("resolution", resolution);
                    return systemAlertRepository.save(alert);
                });
    }

    public Mono<Boolean> isServiceInCriticalState(String serviceId) {
        log.info("Checking critical state for service: {}", serviceId);
        return systemAlertRepository.findByServiceId(serviceId)
                .filter(alert -> alert.getStatus() == SystemAlert.AlertStatus.ACTIVE &&
                        alert.getSeverity() == SystemAlert.AlertSeverity.CRITICAL)
                .hasElements();
    }
} 