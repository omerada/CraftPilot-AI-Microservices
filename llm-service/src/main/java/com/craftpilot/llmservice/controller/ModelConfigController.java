package com.craftpilot.llmservice.controller;

import com.craftpilot.llmservice.dto.ModelConfigDto;
import com.craftpilot.llmservice.service.ModelConfigService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/config")
@RequiredArgsConstructor
@Slf4j 
public class ModelConfigController {
    private final ModelConfigService modelConfigService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ResourceLoader resourceLoader;
    
    // Resource klasörünü referans eden revize edilmiş değer
    @Value("classpath:availableModels.json")
    private Resource modelsResource;
    
    private Map<String, List<String>> categorizedModels;
    
    @PostConstruct
    public void init() {
        // Uygulamanın başlangıcında modelleri yükle
       // loadModelsFromResource();
    }
    
    private void loadModelsFromResource() {
        try {
            log.info("Desteklenen modeller Resource klasöründen yükleniyor...");
            categorizedModels = readModelsFromResource();
            log.info("Modeller başarıyla yüklendi. Toplam kategori sayısı: {}", categorizedModels.size());
        } catch (Exception e) {
            log.error("Model dosyası yüklenirken hata: {}", e.getMessage());
            // Hata durumunda varsayılan modeller kullanılacak
            initializeDefaultModels();
        }
    }
    
    private void initializeDefaultModels() {
        log.info("Varsayılan model listesi kullanılıyor");
        categorizedModels = new HashMap<>();
        categorizedModels.put("openai", Arrays.asList("gpt-4o", "gpt-4-turbo", "gpt-3.5-turbo"));
        categorizedModels.put("google", Arrays.asList("google/gemini-ultra", "google/gemini-pro", "google/gemini-flash"));
        categorizedModels.put("anthropic", Arrays.asList("anthropic/claude-3-opus", "anthropic/claude-3-sonnet", "anthropic/claude-3-haiku"));
        categorizedModels.put("meta", Arrays.asList("meta/llama3-70b-instruct", "meta/llama3-8b-instruct"));
        categorizedModels.put("mistral", Arrays.asList("mistral/mistral-large", "mistral/mistral-medium", "mistral/mistral-small"));
    }

    /**
     * Belirli bir model için varsayılan yapılandırmayı döndürür
     */
    @GetMapping("/model-defaults")
    public Mono<ResponseEntity<ModelConfigDto>> getModelDefaults(
            @RequestParam(value = "model", required = false) String modelId) {
        
        log.info("Model yapılandırması istendi: {}", modelId != null ? modelId : "varsayılan");
        
        return modelConfigService.getDefaultModelConfig(modelId)
                .map(config -> {
                    log.debug("Model yapılandırması bulundu: {}", config);
                    return ResponseEntity.ok(config);
                })
                .onErrorResume(error -> {
                    log.error("Model yapılandırması alınırken hata: {}", error.getMessage());
                    // Hata durumunda varsayılan config ile devam et
                    ModelConfigDto fallbackConfig = ModelConfigDto.builder()
                            .model(modelId != null ? modelId : "google/gemini-pro")
                            .temperature(0.7)
                            .maxTokens(8000)
                            .build();
                    return Mono.just(ResponseEntity.ok(fallbackConfig));
                })
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
    
    /**
     * Desteklenen tüm modeller için yapılandırmaları döndürür
     */
    @GetMapping("/models")
    public Mono<ResponseEntity<Map<String, Object>>> getSupportedModels() {
        log.info("Desteklenen modeller listesi istendi");
        
        try {
            Map<String, Object> response = new HashMap<>();
            
            // Ön yüklenmiş model listesini kullan
            if (categorizedModels == null) {
                // Eğer henüz yüklenmemişse (bu normalde olmamalı), varsayılan listeyi oluştur
                initializeDefaultModels();
            }
            
            // Tüm kategorileri yanıta ekle
            for (Map.Entry<String, List<String>> entry : categorizedModels.entrySet()) {
                response.put(entry.getKey(), entry.getValue().toArray(new String[0]));
            }
            
            return Mono.just(ResponseEntity.ok(response));
        } catch (Exception e) {
            log.error("Modeller listelenirken hata: {}", e.getMessage());
            return Mono.just(ResponseEntity.status(500).build());
        }
    }
    
    /**
     * Resource klasöründen (classpath) modelleri okur ve kategorilere göre gruplar
     */
    private Map<String, List<String>> readModelsFromResource() {
        Map<String, List<String>> result = new HashMap<>();
        Map<String, Set<String>> modelsByProvider = new HashMap<>();
        
        try {
            // Resource'dan JSON'ı oku
            String jsonContent;
            
            if (modelsResource.exists()) {
                // Spring Resource API kullanarak içeriği oku
                try (InputStream inputStream = modelsResource.getInputStream()) {
                    jsonContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                    log.info("Model JSON dosyası resource klasöründen başarıyla okundu");
                }
            } else {
                // Resource bulunamazsa, alternatif olarak ClassPathResource dene
                log.warn("Belirtilen resource bulunamadı, alternatif kaynak deneniyor");
                ClassPathResource alternativeResource = new ClassPathResource("availableModels.json");
                try (InputStream inputStream = alternativeResource.getInputStream()) {
                    jsonContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                    log.info("Model JSON dosyası alternatif kaynak kullanılarak okundu");
                }
            }
            
            // JSON'ı parse et
            JsonNode rootNode = objectMapper.readTree(jsonContent);
            JsonNode dataNode = rootNode.get("data");
            
            // Desteklenen kategorileri tanımla
            Set<String> supportedProviders = new HashSet<>(Arrays.asList(
                    "openai", "google", "anthropic", "meta", "mistral", "cohere", 
                    "qwen", "claude", "llama", "deepseek", "microsoft"));
            
            if (dataNode != null && dataNode.isArray()) {
                log.info("JSON'dan {} model bulundu", dataNode.size());
                
                for (JsonNode modelNode : dataNode) {
                    if (modelNode.has("id")) {
                        String modelId = modelNode.get("id").asText();
                        
                        // Model ID'sinden sağlayıcıyı belirle
                        String provider = extractProviderFromModelId(modelId);
                        
                        // Desteklenen sağlayıcı ise ekle
                        if (supportedProviders.contains(provider)) {
                            modelsByProvider.computeIfAbsent(provider, k -> new HashSet<>()).add(modelId);
                        }
                    }
                }
                
                log.info("Toplam {} farklı sağlayıcı için model bulundu", modelsByProvider.size());
            } else {
                log.warn("JSON dosyasında 'data' alanı bulunamadı veya dizi değil");
                initializeDefaultModels();
                return categorizedModels;
            }
            
            // Her kategori için modelleri liste halinde ayarla
            for (Map.Entry<String, Set<String>> entry : modelsByProvider.entrySet()) {
                String provider = entry.getKey();
                Set<String> models = entry.getValue();
                
                // Modelleri listeye dönüştür ve en popüler/güncel 10 modeli seç
                List<String> modelList = new ArrayList<>(models);
                Collections.sort(modelList); // Basit alfabetik sıralama
                
                // En fazla 10 model göster
                result.put(provider, modelList.subList(0, Math.min(modelList.size(), 10)));
                log.debug("Sağlayıcı '{}' için {} model eklendi", provider, Math.min(modelList.size(), 10));
            }
            
            // Eğer hiç model bulunamazsa, varsayılan modelleri kullan
            if (result.isEmpty()) {
                log.warn("Hiç model bulunamadı, varsayılan modeller kullanılacak");
                initializeDefaultModels();
                return categorizedModels;
            }
            
            return result;
        } catch (Exception e) {
            log.error("JSON dosyasından model okuma hatası: {}", e.getMessage(), e);
            // Hata durumunda varsayılan modelleri döndür
            initializeDefaultModels();
            return categorizedModels;
        }
    }
    
    /**
     * Model ID'sinden sağlayıcı adını çıkarır
     */
    private String extractProviderFromModelId(String modelId) {
        // Model ID formatları: "provider/model-name" veya "model-name"
        if (modelId.contains("/")) {
            return modelId.split("/")[0].toLowerCase();
        }
        
        // Özel durumlar için kontroller
        if (modelId.startsWith("gpt-")) {
            return "openai";
        } else if (modelId.contains("claude")) {
            return "anthropic";
        } else if (modelId.contains("llama")) {
            return "meta";
        } else if (modelId.contains("gemini")) {
            return "google";
        } else if (modelId.contains("mistral")) {
            return "mistral";
        } else if (modelId.contains("phi")) {
            return "microsoft";
        } else if (modelId.contains("qwen")) {
            return "qwen";
        } else if (modelId.contains("command")) {
            return "cohere";
        } else if (modelId.contains("falcon")) {
            return "tii";
        }
        
        // Belirlenemeyen durumlarda model ID'sinin ilk parçasını kullan
        String[] parts = modelId.split("[\\-/]");
        return parts.length > 0 ? parts[0].toLowerCase() : "other";
    }
}
