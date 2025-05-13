package com.craftpilot.adminservice.repository;

import com.craftpilot.adminservice.model.SystemMetrics;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@DataMongoTest
@Testcontainers
@Tag("integration")
class SystemMetricsRepositoryTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0.8");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private SystemMetricsRepository repository;

    @Test
    void testSave() {
        // Given
        Map<String, Double> resourceUsage = new HashMap<>();
        resourceUsage.put("cpu", 45.5);
        resourceUsage.put("memory", 60.2);

        SystemMetrics metrics = SystemMetrics.builder()
                .serviceId("service-1")
                .serviceType(SystemMetrics.ServiceType.API_GATEWAY)
                .status(SystemMetrics.ServiceStatus.HEALTHY)
                .resourceUsage(resourceUsage)
                .timestamp(LocalDateTime.now())
                .build();

        // When
        Mono<SystemMetrics> savedMetrics = repository.save(metrics);

        // Then
        StepVerifier.create(savedMetrics)
                .expectNextMatches(saved -> saved.getServiceId().equals("service-1") &&
                        saved.getServiceType() == SystemMetrics.ServiceType.API_GATEWAY &&
                        saved.getStatus() == SystemMetrics.ServiceStatus.HEALTHY &&
                        saved.getResourceUsage().get("cpu").equals(45.5) &&
                        saved.getId() != null)
                .verifyComplete();
    }

    @Test
    void testFindByServiceId() {
        // Given
        String serviceId = "user-service-1";
        SystemMetrics metrics = SystemMetrics.builder()
                .serviceId(serviceId)
                .serviceType(SystemMetrics.ServiceType.USER_SERVICE)
                .status(SystemMetrics.ServiceStatus.HEALTHY)
                .timestamp(LocalDateTime.now())
                .build();

        // When
        Mono<SystemMetrics> findResult = repository.save(metrics)
                .then(repository.findByServiceId(serviceId));

        // Then
        StepVerifier.create(findResult)
                .expectNextMatches(found -> found.getServiceId().equals(serviceId) &&
                        found.getServiceType() == SystemMetrics.ServiceType.USER_SERVICE)
                .verifyComplete();
    }

    @Test
    void testFindByStatus() {
        // Given
        SystemMetrics metrics = SystemMetrics.builder()
                .serviceId("service-down")
                .serviceType(SystemMetrics.ServiceType.LLM_SERVICE)
                .status(SystemMetrics.ServiceStatus.DOWN)
                .timestamp(LocalDateTime.now())
                .build();

        // When
        Mono<SystemMetrics> saveResult = repository.save(metrics);

        // Then
        StepVerifier.create(saveResult.then(repository.findByStatus(SystemMetrics.ServiceStatus.DOWN).next()))
                .expectNextMatches(found -> found.getServiceId().equals("service-down") &&
                        found.getStatus() == SystemMetrics.ServiceStatus.DOWN)
                .verifyComplete();
    }

    @Test
    void testDelete() {
        // Given
        SystemMetrics metrics = SystemMetrics.builder()
                .serviceId("service-to-delete")
                .serviceType(SystemMetrics.ServiceType.NOTIFICATION_SERVICE)
                .status(SystemMetrics.ServiceStatus.MAINTENANCE)
                .timestamp(LocalDateTime.now())
                .build();

        // When & Then
        StepVerifier.create(
                repository.save(metrics)
                        .flatMap(saved -> repository.deleteById(saved.getId())
                                .then(repository.findById(saved.getId()))))
                .verifyComplete(); // findById should return empty after deletion
    }
}