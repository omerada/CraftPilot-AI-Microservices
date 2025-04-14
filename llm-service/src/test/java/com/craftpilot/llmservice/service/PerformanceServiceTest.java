package com.craftpilot.llmservice.service;

import com.craftpilot.llmservice.cache.PerformanceAnalysisCache;
import com.craftpilot.llmservice.model.performance.PerformanceAnalysisRequest;
import com.craftpilot.llmservice.model.performance.PerformanceAnalysisResponse;
import com.craftpilot.llmservice.model.performance.SuggestionsRequest;
import com.craftpilot.llmservice.repository.PerformanceAnalysisRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.HashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class PerformanceServiceTest {
    
    private PerformanceService performanceService;
    
    @Mock
    private LighthouseService lighthouseService;
    
    @Mock
    private PerformanceAnalysisRepository performanceAnalysisRepository;
    
    @Mock
    private PerformanceAnalysisCache performanceAnalysisCache;
    
    @Mock
    private AiService aiService;
    
    @Mock
    private PromptService promptService;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        performanceService = new PerformanceService(
                lighthouseService, 
                performanceAnalysisRepository, 
                performanceAnalysisCache, 
                aiService, 
                promptService,
                new SimpleMeterRegistry()
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
    void generateSuggestions_ReturnsAiResponse() {
        // Arrange
        SuggestionsRequest request = new SuggestionsRequest();
        request.setAnalysisData(new HashMap<String, Object>());
        
        when(promptService.getPerformanceAnalysisPrompt(any())).thenReturn("test prompt");
        when(aiService.generateAiResponse(anyString())).thenReturn(Mono.just("AI response"));
        
        // Act & Assert
        StepVerifier.create(performanceService.generateSuggestions(request))
                .expectNextMatches(response -> "AI response".equals(response.getContent()))
                .verifyComplete();
    }
}
