package com.craftpilot.adminservice.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SystemMetricsTest {

    @Test
    void testSystemMetricsBuilder() {
        // Given
        String id = "test-id";
        String serviceId = "service-1";
        Map<String, Double> resourceUsage = new HashMap<>();
        resourceUsage.put("cpu", 75.5);
        resourceUsage.put("memory", 80.0);

        Map<String, Long> requestMetrics = new HashMap<>();
        requestMetrics.put("total", 1000L);
        requestMetrics.put("success", 950L);

        LocalDateTime now = LocalDateTime.now();

        // When
        SystemMetrics metrics = SystemMetrics.builder()
                .id(id)
                .serviceId(serviceId)
                .serviceType(SystemMetrics.ServiceType.API_GATEWAY)
                .status(SystemMetrics.ServiceStatus.HEALTHY)
                .resourceUsage(resourceUsage)
                .requestMetrics(requestMetrics)
                .timestamp(now)
                .build();

        // Then
        assertEquals(id, metrics.getId());
        assertEquals(serviceId, metrics.getServiceId());
        assertEquals(SystemMetrics.ServiceType.API_GATEWAY, metrics.getServiceType());
        assertEquals(SystemMetrics.ServiceStatus.HEALTHY, metrics.getStatus());
        assertEquals(resourceUsage, metrics.getResourceUsage());
        assertEquals(requestMetrics, metrics.getRequestMetrics());
        assertEquals(now, metrics.getTimestamp());
    }

    @Test
    void testSystemMetricsEquality() {
        // Given
        SystemMetrics metrics1 = SystemMetrics.builder()
                .id("test-id")
                .serviceId("service-1")
                .build();

        SystemMetrics metrics2 = SystemMetrics.builder()
                .id("test-id")
                .serviceId("service-1")
                .build();

        SystemMetrics metrics3 = SystemMetrics.builder()
                .id("test-id-2")
                .serviceId("service-2")
                .build();

        // Then
        assertEquals(metrics1, metrics2);
        assertNotEquals(metrics1, metrics3);
    }

    @Test
    void testServiceTypeValues() {
        // Then
        assertNotNull(SystemMetrics.ServiceType.values());
        assertTrue(SystemMetrics.ServiceType.values().length > 0);
    }

    @Test
    void testServiceStatusValues() {
        // Then
        assertNotNull(SystemMetrics.ServiceStatus.values());
        assertTrue(SystemMetrics.ServiceStatus.values().length > 0);
    }
} 