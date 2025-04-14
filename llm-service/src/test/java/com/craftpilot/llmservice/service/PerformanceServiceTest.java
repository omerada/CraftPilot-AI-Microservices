package com.craftpilot.llmservice.service;

import com.craftpilot.llmservice.cache.PerformanceAnalysisCache;
import com.craftpilot.llmservice.model.AIRequest;
import com.craftpilot.llmservice.model.AIResponse;
import com.craftpilot.llmservice.model.performance.PerformanceAnalysisRequest;
import com.craftpilot.llmservice.model.performance.PerformanceAnalysisResponse;
import com.craftpilot.llmservice.model.performance.SuggestionsRequest;
import com.craftpilot.llmservice.repository.PerformanceAnalysisRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PerformanceServiceTest {
    
    private PerformanceService performanceService;
    
    @Mock
    private LighthouseService lighthouseService;
    
    @Mock
    private PerformanceAnalysisRepository performanceAnalysisRepository;
    
    @Mock
    private PerformanceAnalysisCache performanceAnalysisCache;
    
    @Mock
    private LLMService llmService;
    
    @Mock
    private PromptService promptService;
    
    @Mock
    private MeterRegistry meterRegistry;
    
    @Mock
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        performanceService = new PerformanceService(
                lighthouseService,
                performanceAnalysisRepository,
                performanceAnalysisCache,
                promptService,
                meterRegistry,
                llmService,
                objectMapper
        );
    }
    
    @Test
    void analyzeWebsite_WhenCacheHit_ReturnsFromCache() {
        // Arrange
        String url = "https://example.com";
        PerformanceAnalysisRequest request = new PerformanceAnalysisRequest();
        request.setUrl(url);
        
        PerformanceAnalysisResponse cachedResponse = PerformanceAnalysisResponse.builder()
                .id("test-id")
                .url(url)
                .performance(0.85)
                .timestamp(Instant.now().toEpochMilli())
                .audits(new HashMap<>())
                .categories(new HashMap<>())
                .build();
        
        when(performanceAnalysisCache.getAnalysisResult(url)).thenReturn(Mono.just(cachedResponse));
        
        // Act & Assert
        StepVerifier.create(performanceService.analyzeWebsite(request))
                .expectNext(cachedResponse)
                .verifyComplete();
    }
    
    @Test
    void analyzeWebsite_WhenCacheMiss_PerformsNewAnalysis() {
        // Arrange
        String url = "https://example.com";
        PerformanceAnalysisRequest request = new PerformanceAnalysisRequest();
        request.setUrl(url);
        
        PerformanceAnalysisResponse newResponse = PerformanceAnalysisResponse.builder()
                .id("test-id")
                .url(url)
                .performance(0.85)
                .timestamp(Instant.now().toEpochMilli())
                .audits(new HashMap<>())
                .categories(new HashMap<>())
                .build();
        
        when(performanceAnalysisCache.getAnalysisResult(url)).thenReturn(Mono.empty());
        when(lighthouseService.analyzeSite(url)).thenReturn(Mono.just(newResponse));
        when(performanceAnalysisRepository.save(any(PerformanceAnalysisResponse.class))).thenReturn(Mono.just(newResponse));
        
        // Act & Assert
        StepVerifier.create(performanceService.analyzeWebsite(request))
                .expectNext(newResponse)
                .verifyComplete();
    }
    
    @Test
    void generateSuggestions_ReturnsAiResponse() throws Exception {
        // Arrange
        SuggestionsRequest request = new SuggestionsRequest();
        request.setAnalysisData(new HashMap<String, Object>());
        request.setUserId("test-user");
        request.setLanguage("tr");
        
        AIResponse aiResponse = AIResponse.builder()
                .response("AI response")
                .success(true)
                .build();
        
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(llmService.processChatCompletion(any(AIRequest.class))).thenReturn(Mono.just(aiResponse));
        
        // Act & Assert
        StepVerifier.create(performanceService.generateSuggestions(request))
                .expectNextMatches(response -> "AI response".equals(response.getContent()))
                .verifyComplete();
    }
}
