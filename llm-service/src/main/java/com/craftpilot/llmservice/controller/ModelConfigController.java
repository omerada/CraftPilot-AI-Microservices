package com.craftpilot.llmservice.controller;

import com.craftpilot.llmservice.dto.ModelConfigDto;
import com.craftpilot.llmservice.service.ModelConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/ai/config")
@RequiredArgsConstructor
public class ModelConfigController {
    private final ModelConfigService modelConfigService;

    /**
     * Belirli bir model için varsayılan yapılandırmayı döndürür
     */
    @GetMapping("/model-defaults")
    public Mono<ResponseEntity<ModelConfigDto>> getModelDefaults(
            @RequestParam(value = "model", required = false) String modelId) {
        return modelConfigService.getDefaultModelConfig(modelId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
    
    /**
     * Desteklenen tüm modeller için yapılandırmaları döndürür
     */
    @GetMapping("/models")
    public Mono<ResponseEntity<Map<String, Object>>> getSupportedModels() {
        Map<String, Object> response = new HashMap<>();
        
        // Popüler model aileleri
        response.put("openai", new String[]{"gpt-4o", "gpt-4-turbo", "gpt-3.5-turbo"});
        response.put("google", new String[]{"google/gemini-ultra", "google/gemini-pro", "google/gemini-flash"});
        response.put("anthropic", new String[]{"anthropic/claude-3-opus", "anthropic/claude-3-sonnet", "anthropic/claude-3-haiku"});
        response.put("meta", new String[]{"meta/llama3-70b-instruct", "meta/llama3-8b-instruct"});
        response.put("mistral", new String[]{"mistral/mistral-large", "mistral/mistral-medium", "mistral/mistral-small"});
        
        return Mono.just(ResponseEntity.ok(response));
    }
}
