package com.craftpilot.adminservice.service;

import com.craftpilot.adminservice.model.SystemMetrics;
import com.craftpilot.adminservice.repository.SystemMetricsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemMetricsService {
    private final SystemMetricsRepository systemMetricsRepository;

    public Mono<SystemMetrics> saveMetrics(SystemMetrics metrics) {
        log.info("Saving system metrics for service: {}", metrics.getServiceId());
        return systemMetricsRepository.save(metrics);
    }

    public Mono<SystemMetrics> getMetricsById(String serviceId) {
        log.info("Retrieving system metrics for service: {}", serviceId);
        return systemMetricsRepository.findById(serviceId);
    }

    public Flux<SystemMetrics> getAllMetrics() {
        log.info("Retrieving all system metrics");
        return systemMetricsRepository.findAll();
    }

    public Flux<SystemMetrics> getMetricsByServiceType(SystemMetrics.ServiceType serviceType) {
        log.info("Retrieving system metrics for service type: {}", serviceType);
        return systemMetricsRepository.findByServiceType(serviceType);
    }

    public Flux<SystemMetrics> getMetricsByStatus(SystemMetrics.ServiceStatus status) {
        log.info("Retrieving system metrics for status: {}", status);
        return systemMetricsRepository.findByStatus(status);
    }

    public Mono<Void> deleteMetrics(String serviceId) {
        log.info("Deleting system metrics for service: {}", serviceId);
        return systemMetricsRepository.deleteById(serviceId);
    }

    public Mono<List<SystemMetrics>> getUnhealthyServices() {
        log.info("Retrieving unhealthy services");
        return systemMetricsRepository.findByStatus(SystemMetrics.ServiceStatus.DEGRADED)
                .concatWith(systemMetricsRepository.findByStatus(SystemMetrics.ServiceStatus.DOWN))
                .collectList();
    }

    public Mono<Boolean> isServiceHealthy(String serviceId) {
        log.info("Checking health status for service: {}", serviceId);
        return systemMetricsRepository.findById(serviceId)
                .map(metrics -> metrics.getStatus() == SystemMetrics.ServiceStatus.HEALTHY)
                .defaultIfEmpty(false);
    }

    public Mono<SystemMetrics> updateServiceStatus(String serviceId, SystemMetrics.ServiceStatus newStatus) {
        log.info("Updating status for service {} to {}", serviceId, newStatus);
        return systemMetricsRepository.findById(serviceId)
                .flatMap(metrics -> {
                    metrics.setStatus(newStatus);
                    return systemMetricsRepository.save(metrics);
                });
    }

    public Mono<List<SystemMetrics>> getServicesRequiringMaintenance() {
        log.info("Retrieving services requiring maintenance");
        return systemMetricsRepository.findByStatus(SystemMetrics.ServiceStatus.MAINTENANCE)
                .collectList();
    }
} 