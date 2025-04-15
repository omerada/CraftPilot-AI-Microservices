package com.craftpilot.llmservice.service;

import com.craftpilot.llmservice.cache.PerformanceAnalysisCache;
import com.craftpilot.llmservice.model.AIResponse;
import com.craftpilot.llmservice.model.performance.*;
import com.craftpilot.llmservice.repository.PerformanceAnalysisRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PerformanceServiceTest {
    
    private PerformanceService performanceService;
    
    @Mock
    private PerformanceAnalysisRepository performanceAnalysisRepository;
    
    @Mock
    private PerformanceAnalysisCache performanceAnalysisCache;
    
    @Mock
    private LLMService llmService;
    
    @Mock
    private PromptService promptService;
    
    @Mock
    private WebClient webClient;
    
    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;
    
    @Mock
    private WebClient.RequestBodySpec requestBodySpec;
    
    @Mock
    private WebClient.ResponseSpec responseSpec;
    
    private MeterRegistry meterRegistry;
    
    @Mock
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        
        performanceService = new PerformanceService(
                performanceAnalysisRepository,
                performanceAnalysisCache,
                promptService,
                meterRegistry,
                llmService,
                objectMapper
        );
        
        // WebClient MockUp
        ReflectionTestUtils.setField(performanceService, "webClient", webClient);
    }
    
    @Test
    void analyzeWebsite_WhenCacheHit_ReturnsFromCache() {
        // Setup
        PerformanceAnalysisRequest request = new PerformanceAnalysisRequest();
        request.setUrl("https://example.com");
        
        PerformanceAnalysisResponse cachedResponse = PerformanceAnalysisResponse.builder()
                .id("cached-id")
                .url("https://example.com")
                .build();
        
        when(performanceAnalysisCache.getAnalysisResult(anyString())).thenReturn(Mono.just(cachedResponse));
        
        // Test
        StepVerifier.create(performanceService.analyzeWebsite(request))
                .expectNext(cachedResponse)
                .verifyComplete();
        
        // Verify
        verify(performanceAnalysisCache).getAnalysisResult("https://example.com");
        verifyNoInteractions(webClient);
        verifyNoInteractions(performanceAnalysisRepository);
    }
    
    @Test
    void analyzeWebsite_WhenCacheMiss_CallsLighthouseService() {
        // Setup
        PerformanceAnalysisRequest request = new PerformanceAnalysisRequest();
        request.setUrl("https://example.com");
        
        PerformanceAnalysisResponse newResponse = PerformanceAnalysisResponse.builder()
                .id("new-id")
                .url("https://example.com")
                .build();
                
        Map<String, Object> queueResponse = new HashMap<>();
        queueResponse.put("jobId", "test-job-id");
        
        Map<String, Object> jobResponse = new HashMap<>();
        jobResponse.put("complete", true);
        jobResponse.put("data", newResponse);
        
        // Cache miss
        when(performanceAnalysisCache.getAnalysisResult(anyString())).thenReturn(Mono.empty());
        
        // WebClient mocking chain
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(queueResponse), Mono.just(jobResponse));
        
        // WebClient mocking for GET (polling)
        when(webClient.get()).thenReturn(requestBodyUriSpec);
        
        // Repository save
        when(performanceAnalysisRepository.save(any(PerformanceAnalysisResponse.class))).thenReturn(Mono.just(newResponse));
        
        // Mock ObjectMapper to return the actual instance for `convertValue` call
        when(objectMapper.convertValue(any(), eq(PerformanceAnalysisResponse.class))).thenReturn(newResponse);
        
        // Override retry behavior for test
        ReflectionTestUtils.setField(performanceService, "lighthouseServiceUrl", "http://test-url");
        
        // Test
        StepVerifier.create(performanceService.analyzeWebsite(request))
                .expectNext(newResponse)
                .verifyComplete();
        
        // Verify
        verify(performanceAnalysisCache).getAnalysisResult("https://example.com");
        verify(performanceAnalysisRepository).save(any(PerformanceAnalysisResponse.class));
        verify(performanceAnalysisCache).cacheAnalysisResult(eq("https://example.com"), any(PerformanceAnalysisResponse.class));
    }
    
    @Test
    void generateSuggestions_ReturnsAiResponse() throws Exception {
        // Setup
        SuggestionsRequest request = new SuggestionsRequest();
        request.setAnalysisData(Map.of("performance", 0.8));
        
        AIResponse aiResponse = AIResponse.builder()
                .response("Suggestion content")
                .build();
        
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(llmService.processChatCompletion(any())).thenReturn(Mono.just(aiResponse));
        
        // Test
        StepVerifier.create(performanceService.generateSuggestions(request))
                .expectNextMatches(response -> response.getContent().equals("Suggestion content"))
                .verifyComplete();
        
        // Verify
        verify(llmService).processChatCompletion(any());
    }
}
