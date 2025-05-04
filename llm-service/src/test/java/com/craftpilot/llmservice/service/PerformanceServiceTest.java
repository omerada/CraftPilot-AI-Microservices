package com.craftpilot.llmservice.service;

import com.craftpilot.llmservice.cache.PerformanceAnalysisCache;
import com.craftpilot.llmservice.model.AIRequest;
import com.craftpilot.llmservice.model.AIResponse;
import com.craftpilot.llmservice.model.StreamResponse;
import com.craftpilot.llmservice.model.performance.*;
import com.craftpilot.llmservice.repository.PerformanceAnalysisRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PerformanceServiceTest {

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
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;
    
    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;
    
    @Mock
    private WebClient.RequestBodySpec requestBodySpec;
    
    @Mock
    private WebClient.ResponseSpec responseSpec;
    
    @Mock
    private WebClient webClient;
    
    private ObjectMapper objectMapper;
    
    private PerformanceService performanceService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        performanceService = new PerformanceService(
                performanceAnalysisRepository,
                performanceAnalysisCache,
                promptService,
                meterRegistry,
                llmService,
                objectMapper
        );
        
        // WebClient mock setup
        // ...existing code...
    }

    @Test
    void testGenerateSuggestions() throws JsonProcessingException {
        // Arrange
        Map<String, Object> analysisData = new HashMap<>();
        analysisData.put("performance", 85.5);
        analysisData.put("key1", "value1");
        analysisData.put("key2", "value2");
        
        SuggestionsRequest request = SuggestionsRequest.builder()
                .userId("user123")
                .model("model1")
                .maxTokens(1000)
                .temperature(0.7)
                .analysisData(analysisData)
                .build();
        
        AIResponse aiResponse = new AIResponse();
        aiResponse.setResponse("AI generated response");
        
        // Burada AnalysisData'yı bir String'e dönüştürmemiz gerekiyor
        String analysisJson = objectMapper.writeValueAsString(request.getAnalysisData());
        
        when(llmService.processChatCompletion(any(AIRequest.class)))
                .thenReturn(Mono.just(aiResponse));
        
        // Act
        Mono<SuggestionsResponse> result = performanceService.generateSuggestions(request);
        
        // Assert
        // ...existing code...
    }

    // Diğer test metodları...
}