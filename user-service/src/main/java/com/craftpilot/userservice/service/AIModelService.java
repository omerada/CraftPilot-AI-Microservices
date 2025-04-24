package com.craftpilot.userservice.service;

import com.craftpilot.userservice.dto.ai.ModelsData;
import com.craftpilot.userservice.model.ai.AIModel;
import com.craftpilot.userservice.model.ai.Provider;
import com.craftpilot.userservice.repository.AIModelRepository;
import com.craftpilot.userservice.repository.ProviderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AIModelService {

    private final AIModelRepository modelRepository;
    private final ProviderRepository providerRepository;
    
    @Value("${ai.models.version:1}")
    private Integer modelsVersion;

    @CircuitBreaker(name = "aiModels", fallbackMethod = "getDefaultModels")
    public Mono<ModelsData> getAvailableModels(String userPlan) {
        log.info("AI modelleri getiriliyor: userPlan={}", userPlan);
        
        // Kullanıcı planına göre uygun modelleri filtrele
        return filterModelsByPlan(userPlan)
            .flatMap(models -> {
                // Provider'ları getir
                return providerRepository.findAll()
                    .collectList()
                    .flatMap(providers -> {
                        // Provider'lara model listesini ekle
                        Map<String, List<AIModel>> modelsByProvider = models.stream()
                            .collect(Collectors.groupingBy(AIModel::getProvider));
                        
                        List<Provider> enrichedProviders = providers.stream()
                            .map(provider -> {
                                List<AIModel> providerModels = modelsByProvider.getOrDefault(provider.getName(), new ArrayList<>());
                                provider.setModels(providerModels);
                                return provider;
                            })
                            .collect(Collectors.toList());
                        
                        return Mono.just(ModelsData.builder()
                            .models(models)
                            .providers(enrichedProviders)
                            .version(modelsVersion)
                            .build()
                        );
                    });
            })
            .doOnSuccess(data -> log.info("AI modelleri başarıyla getirildi: modelCount={}, providerCount={}", 
                data.getModels().size(), data.getProviders().size()))
            .doOnError(e -> log.error("AI modelleri getirilirken hata: error={}", e.getMessage()));
    }
    
    public ModelsData getDefaultModels(String userPlan, Throwable t) {
        log.warn("Varsayılan AI modeller döndürülüyor (fallback): userPlan={}, error={}", userPlan, t.getMessage());
        
        // Basit varsayılan modeller oluştur
        List<AIModel> defaultModels = new ArrayList<>();
        defaultModels.add(AIModel.builder()
            .id("google/gemini-2.0-flash-lite-001")
            .modelId("google/gemini-2.0-flash-lite-001")
            .modelName("Gemini Flash Lite")
            .provider("Google") 
            .maxInputTokens(6000)
            .requiredPlan("free")
            .build());
        
        // Birkaç varsayılan provider oluştur
        List<Provider> defaultProviders = new ArrayList<>();
        defaultProviders.add(Provider.builder()
            .name("Google")
            .icon("TbBrandGoogle")
            .description("Gemini serisi modeller")
            .models(defaultModels)
            .build());
            
        return ModelsData.builder()
            .models(defaultModels)
            .providers(defaultProviders)
            .version(1)
            .build();
    }
    
    public String getDefaultModelForPlan(String userPlan) {
        switch(userPlan.toLowerCase()) {
            case "premium":
            case "enterprise":
                return "google/gemini-2.0-pro-001";
            case "pro":
                return "google/gemini-2.0-pro-001";
            case "free":
            default:
                return "google/gemini-2.0-flash-lite-001";
        }
    }
    
    private Mono<List<AIModel>> filterModelsByPlan(String userPlan) {
        // Plan hiyerarşisi - üst plan, alt planları içerir
        Map<String, List<String>> planHierarchy = new HashMap<>();
        planHierarchy.put("free", List.of("free"));
        planHierarchy.put("pro", List.of("free", "pro"));
        planHierarchy.put("premium", List.of("free", "pro", "premium"));
        planHierarchy.put("enterprise", List.of("free", "pro", "premium", "enterprise"));
        
        List<String> allowedPlans = planHierarchy.getOrDefault(userPlan.toLowerCase(), List.of("free"));
        
        return modelRepository.findAll()
            .filter(model -> allowedPlans.contains(model.getRequiredPlan().toLowerCase()))
            .collectList();
    }
    
    private String mapLegacyModelId(String modelId) {
        // Eski ID'den yeni ID'ye eşleme
        Map<String, String> modelMappings = new HashMap<>();
        modelMappings.put("gemini-pro", "google/gemini-1.5-pro");
        modelMappings.put("gpt-4-turbo", "openai/gpt-4-turbo");
        
        // Eşleşen bir eski ID varsa yeni ID'yi döndür, yoksa orijinal ID'yi kullan
        return modelMappings.getOrDefault(modelId, modelId);
    }
    
    public Mono<AIModel> saveModel(AIModel model) {
        log.info("AI Model kaydediliyor: modelId={}", model.getModelId() != null ? model.getModelId() : model.getId());
        return modelRepository.save(model);
    }

    public Flux<AIModel> saveAllModels(List<AIModel> models) {
        log.info("{} AI model kaydediliyor", models.size());
        return Flux.fromIterable(models)
            .flatMap(this::saveModel);
    }
}
