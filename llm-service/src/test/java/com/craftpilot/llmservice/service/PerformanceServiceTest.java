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
        
        // WebClient'ı test sınıfına enjekte et
        ReflectionTestUtils.setField(performanceService, "webClient", webClient);
        
        // Lighthouse servis URL'ini test için geçersiz kıl
        ReflectionTestUtils.setField(performanceService, "lighthouseServiceUrl", "http://test-url");
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
