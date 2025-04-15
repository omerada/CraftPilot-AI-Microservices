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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

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
    
    @SuppressWarnings("unchecked")
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
        
        // IMPROVED: WebClient MOCK - Functional style API kullanarak zinciri mockla
        // POST isteği ayarla
        WebClient.RequestBodyUriSpec postSpec = mock(WebClient.RequestBodyUriSpec.class);
        when(webClient.post()).thenReturn(postSpec);
        
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        when(postSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.contentType(any())).thenReturn(bodySpec);
        when(bodySpec.bodyValue(any())).thenReturn(bodySpec);
        
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        when(bodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(eq(Map.class))).thenReturn(Mono.just(queueResponse));
        
        // GET isteği ayarla
        WebClient.RequestHeadersUriSpec<?> getSpec = mock(WebClient.RequestHeadersUriSpec.class);
        when(webClient.get()).thenReturn(getSpec);
        
        WebClient.RequestHeadersSpec<?> headersSpec = mock(WebClient.RequestHeadersSpec.class);
        when(getSpec.uri(contains("test-job-id"))).thenReturn(headersSpec);
        
        WebClient.ResponseSpec getResponseSpec = mock(WebClient.ResponseSpec.class);
        when(headersSpec.retrieve()).thenReturn(getResponseSpec);
        when(getResponseSpec.bodyToMono(eq(Map.class))).thenReturn(Mono.just(jobResponse));
        
        // Repository save
        when(performanceAnalysisRepository.save(any(PerformanceAnalysisResponse.class))).thenReturn(Mono.just(newResponse));
        
        // Mock ObjectMapper 
        when(objectMapper.convertValue(any(), eq(PerformanceAnalysisResponse.class))).thenReturn(newResponse);
        
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
