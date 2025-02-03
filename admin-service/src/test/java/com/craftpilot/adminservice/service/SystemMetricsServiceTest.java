package com.craftpilot.adminservice.service;

import com.craftpilot.adminservice.model.SystemMetrics;
import com.craftpilot.adminservice.repository.SystemMetricsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemMetricsServiceTest {

    @Mock
    private SystemMetricsRepository repository;

    private SystemMetricsService service;

    @BeforeEach
    void setUp() {
        service = new SystemMetricsService(repository);
    }

    @Test
    void testSaveMetrics() {
        // Given
        SystemMetrics metrics = SystemMetrics.builder()
                .id("test-id")
                .serviceId("service-1")
                .build();

        when(repository.save(any(SystemMetrics.class))).thenReturn(Mono.just(metrics));

        // When & Then
        StepVerifier.create(service.saveMetrics(metrics))
                .expectNext(metrics)
                .verifyComplete();

        verify(repository).save(metrics);
    }

    @Test
    void testGetMetricsById() {
        // Given
        SystemMetrics metrics = SystemMetrics.builder()
                .id("test-id")
                .serviceId("service-1")
                .build();

        when(repository.findById("test-id")).thenReturn(Mono.just(metrics));

        // When & Then
        StepVerifier.create(service.getMetricsById("test-id"))
                .expectNext(metrics)
                .verifyComplete();

        verify(repository).findById("test-id");
    }

    @Test
    void testGetAllMetrics() {
        // Given
        SystemMetrics metrics1 = SystemMetrics.builder().id("1").build();
        SystemMetrics metrics2 = SystemMetrics.builder().id("2").build();
        List<SystemMetrics> metricsList = Arrays.asList(metrics1, metrics2);

        when(repository.findAll()).thenReturn(Flux.fromIterable(metricsList));

        // When & Then
        StepVerifier.create(service.getAllMetrics())
                .expectNext(metrics1, metrics2)
                .verifyComplete();

        verify(repository).findAll();
    }

    @Test
    void testGetMetricsByServiceType() {
        // Given
        SystemMetrics metrics = SystemMetrics.builder()
                .id("test-id")
                .serviceType(SystemMetrics.ServiceType.API_GATEWAY)
                .build();

        when(repository.findByServiceType(SystemMetrics.ServiceType.API_GATEWAY))
                .thenReturn(Flux.just(metrics));

        // When & Then
        StepVerifier.create(service.getMetricsByServiceType(SystemMetrics.ServiceType.API_GATEWAY))
                .expectNext(metrics)
                .verifyComplete();

        verify(repository).findByServiceType(SystemMetrics.ServiceType.API_GATEWAY);
    }

    @Test
    void testGetUnhealthyServices() {
        // Given
        SystemMetrics degradedMetrics = SystemMetrics.builder()
                .id("1")
                .status(SystemMetrics.ServiceStatus.DEGRADED)
                .build();
        SystemMetrics downMetrics = SystemMetrics.builder()
                .id("2")
                .status(SystemMetrics.ServiceStatus.DOWN)
                .build();

        when(repository.findByStatus(SystemMetrics.ServiceStatus.DEGRADED))
                .thenReturn(Flux.just(degradedMetrics));
        when(repository.findByStatus(SystemMetrics.ServiceStatus.DOWN))
                .thenReturn(Flux.just(downMetrics));

        // When & Then
        StepVerifier.create(service.getUnhealthyServices())
                .expectNext(Arrays.asList(degradedMetrics, downMetrics))
                .verifyComplete();

        verify(repository).findByStatus(SystemMetrics.ServiceStatus.DEGRADED);
        verify(repository).findByStatus(SystemMetrics.ServiceStatus.DOWN);
    }

    @Test
    void testIsServiceHealthy() {
        // Given
        SystemMetrics healthyMetrics = SystemMetrics.builder()
                .id("test-id")
                .status(SystemMetrics.ServiceStatus.HEALTHY)
                .build();

        when(repository.findById("test-id")).thenReturn(Mono.just(healthyMetrics));

        // When & Then
        StepVerifier.create(service.isServiceHealthy("test-id"))
                .expectNext(true)
                .verifyComplete();

        verify(repository).findById("test-id");
    }

    @Test
    void testUpdateServiceStatus() {
        // Given
        SystemMetrics metrics = SystemMetrics.builder()
                .id("test-id")
                .status(SystemMetrics.ServiceStatus.HEALTHY)
                .build();

        SystemMetrics updatedMetrics = SystemMetrics.builder()
                .id("test-id")
                .status(SystemMetrics.ServiceStatus.DEGRADED)
                .build();

        when(repository.findById("test-id")).thenReturn(Mono.just(metrics));
        when(repository.save(any(SystemMetrics.class))).thenReturn(Mono.just(updatedMetrics));

        // When & Then
        StepVerifier.create(service.updateServiceStatus("test-id", SystemMetrics.ServiceStatus.DEGRADED))
                .expectNext(updatedMetrics)
                .verifyComplete();

        verify(repository).findById("test-id");
        verify(repository).save(any(SystemMetrics.class));
    }
} 