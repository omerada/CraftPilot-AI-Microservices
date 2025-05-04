package com.craftpilot.llmservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class PerformanceServiceTest {

    @Mock
    private WebClient webClient;

    @InjectMocks
    private PerformanceService performanceService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetPerformanceData() {
        // Mock response
        Map<String, Object> responseMap = Map.of("key", "value");

        // Corrected code
        when(webClient.get().uri(anyString()).retrieve().bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {}))
            .thenReturn(Mono.just(responseMap));

        // Test the method
        Map<String, Object> result = performanceService.getPerformanceData("someUri");
        assertEquals(responseMap, result);
    }

    @Test
    void testGetPerformanceData_withException() {
        // Mock exception
        when(webClient.get().uri(anyString()).retrieve().bodyToMono(eq(Map.class)))
            .thenThrow(WebClientResponseException.class);

        // Test the method
        assertThrows(WebClientResponseException.class, () -> performanceService.getPerformanceData("someUri"));
    }
}