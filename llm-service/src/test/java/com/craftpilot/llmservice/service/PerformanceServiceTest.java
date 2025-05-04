package com.craftpilot.llmservice.service;

import com.craftpilot.llmservice.cache.PerformanceAnalysisCache;
import com.craftpilot.llmservice.repository.PerformanceAnalysisRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PerformanceServiceTest {

    @Mock
    private WebClient webClient;
    
    @Mock
    private PerformanceAnalysisRepository performanceAnalysisRepository;
    
    @Mock
    private PerformanceAnalysisCache performanceAnalysisCache;
    
    @Mock
    private PromptService promptService;
    
    @Mock
    private MeterRegistry meterRegistry;
    
    @Mock
    private LLMService llmService;
    
    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private PerformanceService performanceService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        performanceService = new PerformanceService(
            performanceAnalysisRepository,
            performanceAnalysisCache,
            promptService,
            meterRegistry,
            llmService,
            objectMapper,
            webClient
        );
    }

    @Test
    void testGetPerformanceData() {
        // Mock response
        Map<String, Object> responseMap = Map.of("key", "value");

        // Mock WebClient chain
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(eq(new ParameterizedTypeReference<Map<String, Object>>() {})))
            .thenReturn(Mono.just(responseMap));

        // Test the method
        Map<String, Object> result = performanceService.getPerformanceData("someUri");
        assertEquals(responseMap, result);
    }

    @Test
    void testGetPerformanceData_withException() {
        // Mock WebClient chain
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(eq(new ParameterizedTypeReference<Map<String, Object>>() {})))
            .thenThrow(WebClientResponseException.class);

        // Test the method expecting an exception to be handled (returns empty map)
        Map<String, Object> result = performanceService.getPerformanceData("someUri");
        assertEquals(0, result.size());
    }
}