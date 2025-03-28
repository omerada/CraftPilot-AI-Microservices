package com.craftpilot.llmservice.service;

import com.craftpilot.llmservice.dto.ModelConfigDto;
import com.craftpilot.llmservice.config.OpenRouterConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class ModelConfigService {
    private final OpenRouterConfig openRouterConfig;
    
    // In-memory model yapılandırma cache'i
    private final Map<String, ModelConfigDto> modelConfigCache = new ConcurrentHashMap<>();
    
    /**
     * Bir model için varsayılan yapılandırmayı döndürür
     */
    public Mono<ModelConfigDto> getDefaultModelConfig(String modelId) {
        // Cache'de varsa hemen döndür
        if (modelConfigCache.containsKey(modelId)) {
            return Mono.just(modelConfigCache.get(modelId));
        }
        
        // Model ID'sine göre uygun yapılandırmayı belirle
        ModelConfigDto config = createModelConfig(modelId);
        
        // Cache'e ekle
        modelConfigCache.put(modelId, config);
        
        return Mono.just(config);
    }
    
    /**
     * Model tipine göre uygun yapılandırmayı oluşturur
     */
    private ModelConfigDto createModelConfig(String modelId) {
        // Modelid null ise varsayılan model kullan
        if (modelId == null || modelId.isEmpty()) {
            modelId = openRouterConfig.getDefaultModel();
        }
        
        // Model ailesini belirle
        if (modelId.contains("gpt-4") || modelId.contains("openai")) {
            return createGpt4Config(modelId);
        } else if (modelId.contains("claude")) {
            return createClaudeConfig(modelId);
        } else if (modelId.contains("gemini") || modelId.contains("google")) {
            return createGeminiConfig(modelId);
        } else if (modelId.contains("mistral")) {
            return createMistralConfig(modelId);
        } else if (modelId.contains("llama")) {
            return createLlamaConfig(modelId);
        } else {
            // Bilinmeyen model için varsayılan değerler
            return createDefaultConfig(modelId);
        }
    }
    
    private ModelConfigDto createGpt4Config(String modelId) {
        // GPT-4 ve türevleri için yapılandırma
        Double temperature = 0.7;
        Integer maxTokens = modelId.contains("gpt-4o") ? 16000 : 8000;
        
        // GPT-4o için vision token limitini ayarla
        if (modelId.contains("vision")) {
            maxTokens = 4000;
        }
        
        return ModelConfigDto.builder()
                .model(modelId)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .build();
    }
    
    private ModelConfigDto createClaudeConfig(String modelId) {
        // Claude serisi için yapılandırma
        Double temperature = 0.5;
        Integer maxTokens;
        
        if (modelId.contains("opus")) {
            maxTokens = 12000;
        } else if (modelId.contains("sonnet")) {
            maxTokens = 8000;
        } else if (modelId.contains("haiku")) {
            maxTokens = 5000;
        } else {
            maxTokens = 8000; // Varsayılan
        }
        
        return ModelConfigDto.builder()
                .model(modelId)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .build();
    }
    
    private ModelConfigDto createGeminiConfig(String modelId) {
        // Gemini serisi için yapılandırma
        Double temperature = 0.5;
        Integer maxTokens;
        
        if (modelId.contains("gemini-ultra") || modelId.contains("pro-vision")) {
            maxTokens = 16000;
        } else if (modelId.contains("flash")) {
            maxTokens = 10000;
        } else {
            maxTokens = 8000; // Varsayılan
        }
        
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put("isGoogle", true);
        
        return ModelConfigDto.builder()
                .model(modelId)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .additionalParams(additionalParams)
                .build();
    }
    
    private ModelConfigDto createMistralConfig(String modelId) {
        // Mistral serisi için yapılandırma
        Double temperature = 0.6;
        Integer maxTokens;
        
        if (modelId.contains("medium")) {
            maxTokens = 8000;
        } else if (modelId.contains("small")) {
            maxTokens = 7000;
        } else if (modelId.contains("large")) {
            maxTokens = 10000;
        } else {
            maxTokens = 8000; // Varsayılan
        }
        
        return ModelConfigDto.builder()
                .model(modelId)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .build();
    }
    
    private ModelConfigDto createLlamaConfig(String modelId) {
        // Llama serisi için yapılandırma
        Double temperature = 0.6;
        Integer maxTokens;
        
        if (modelId.contains("70b")) {
            maxTokens = 10000;
        } else if (modelId.contains("13b")) {
            maxTokens = 6000;
        } else {
            maxTokens = 4000; // Varsayılan
        }
        
        return ModelConfigDto.builder()
                .model(modelId)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .build();
    }
    
    private ModelConfigDto createDefaultConfig(String modelId) {
        // Varsayılan yapılandırma
        return ModelConfigDto.builder()
                .model(modelId)
                .temperature(openRouterConfig.getTemperature())
                .maxTokens(openRouterConfig.getMaxTokens())
                .build();
    }
}
