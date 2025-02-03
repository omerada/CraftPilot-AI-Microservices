package com.craftpilot.adminservice.controller;

import com.craftpilot.adminservice.model.SystemMetrics;
import com.craftpilot.adminservice.service.SystemMetricsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemMetricsControllerTest {

    @Mock
    private SystemMetricsService service;

    @InjectMocks
    private SystemMetricsController controller;

    @Test
    void testSaveMetrics() {
        // Given
        SystemMetrics metrics = SystemMetrics.builder()
                .id("test-id")
                .serviceId("service-1")
                .build();

        when(service.saveMetrics(any(SystemMetrics.class))).thenReturn(Mono.just(metrics));

        // When & Then
        StepVerifier.create(controller.saveMetrics(metrics))
                .expectNext(ResponseEntity.ok(metrics))
                .verifyComplete();

        verify(service).saveMetrics(metrics);
    }

    @Test
    void testGetMetricsById() {
        // Given
        SystemMetrics metrics = SystemMetrics.builder()
                .id("test-id")
                .serviceId("service-1")
                .build();

        when(service.getMetricsById("test-id")).thenReturn(Mono.just(metrics));

        // When & Then
        StepVerifier.create(controller.getMetricsById("test-id"))
                .expectNext(ResponseEntity.ok(metrics))
                .verifyComplete();

        verify(service).getMetricsById("test-id");
    }

    @Test
    void testGetAllMetrics() {
        // Given
        SystemMetrics metrics1 = SystemMetrics.builder().id("1").build();
        SystemMetrics metrics2 = SystemMetrics.builder().id("2").build();

        when(service.getAllMetrics()).thenReturn(Flux.just(metrics1, metrics2));

        // When & Then
        StepVerifier.create(controller.getAllMetrics())
                .expectNext(metrics1, metrics2)
                .verifyComplete();

        verify(service).getAllMetrics();
    }

    @Test
    void testGetMetricsByServiceType() {
        // Given
        SystemMetrics metrics = SystemMetrics.builder()
                .id("test-id")
                .serviceType(SystemMetrics.ServiceType.API_GATEWAY)
                .build();

        when(service.getMetricsByServiceType(SystemMetrics.ServiceType.API_GATEWAY))
                .thenReturn(Flux.just(metrics));

        // When & Then
        StepVerifier.create(controller.getMetricsByServiceType(SystemMetrics.ServiceType.API_GATEWAY))
                .expectNext(metrics)
                .verifyComplete();

        verify(service).getMetricsByServiceType(SystemMetrics.ServiceType.API_GATEWAY);
    }

    @Test
    void testGetUnhealthyServices() {
        // Given
        SystemMetrics metrics1 = SystemMetrics.builder()
                .id("1")
                .status(SystemMetrics.ServiceStatus.DEGRADED)
                .build();
        SystemMetrics metrics2 = SystemMetrics.builder()
                .id("2")
                .status(SystemMetrics.ServiceStatus.DOWN)
                .build();
        List<SystemMetrics> unhealthyServices = Arrays.asList(metrics1, metrics2);

        when(service.getUnhealthyServices()).thenReturn(Mono.just(unhealthyServices));

        // When & Then
        StepVerifier.create(controller.getUnhealthyServices())
                .expectNext(ResponseEntity.ok(unhealthyServices))
                .verifyComplete();

        verify(service).getUnhealthyServices();
    }

    @Test
    void testIsServiceHealthy() {
        // Given
        when(service.isServiceHealthy("test-id")).thenReturn(Mono.just(true));

        // When & Then
        StepVerifier.create(controller.isServiceHealthy("test-id"))
                .expectNext(ResponseEntity.ok(true))
                .verifyComplete();

        verify(service).isServiceHealthy("test-id");
    }

    @Test
    void testUpdateServiceStatus() {
        // Given
        SystemMetrics updatedMetrics = SystemMetrics.builder()
                .id("test-id")
                .status(SystemMetrics.ServiceStatus.DEGRADED)
                .build();

        when(service.updateServiceStatus("test-id", SystemMetrics.ServiceStatus.DEGRADED))
                .thenReturn(Mono.just(updatedMetrics));

        // When & Then
        StepVerifier.create(controller.updateServiceStatus("test-id", SystemMetrics.ServiceStatus.DEGRADED))
                .expectNext(ResponseEntity.ok(updatedMetrics))
                .verifyComplete();

        verify(service).updateServiceStatus("test-id", SystemMetrics.ServiceStatus.DEGRADED);
    }

    @Test
    void testGetServicesRequiringMaintenance() {
        // Given
        SystemMetrics metrics = SystemMetrics.builder()
                .id("test-id")
                .status(SystemMetrics.ServiceStatus.MAINTENANCE)
                .build();
        List<SystemMetrics> maintenanceServices = List.of(metrics);

        when(service.getServicesRequiringMaintenance()).thenReturn(Mono.just(maintenanceServices));

        // When & Then
        StepVerifier.create(controller.getServicesRequiringMaintenance())
                .expectNext(ResponseEntity.ok(maintenanceServices))
                .verifyComplete();

        verify(service).getServicesRequiringMaintenance();
    }
} 