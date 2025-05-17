package com.craftpilot.userservice.service;

import com.craftpilot.userservice.dto.ai.ModelsData;
import com.craftpilot.userservice.model.ai.AIModel;
import com.craftpilot.userservice.model.ai.Provider;
import com.craftpilot.userservice.repository.AIModelRepository;
import com.craftpilot.userservice.repository.ProviderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AIModelService {

    private final AIModelRepository modelRepository;
    private final ProviderRepository providerRepository;
    
    @CircuitBreaker(name = "ai_models", fallbackMethod = "getDefaultModels")
    public Mono<ModelsData> getAvailableModels(String userPlan) {
        log.info("Tüm AI modeller getiriliyor (filtreleme yapılmadan)");
        
        return modelRepository.findAll()
            .collectList()
            .flatMap(models -> {
                // Modelleri önce kredi tipine (STANDARD -> ADVANCED) sonra maliyete (ucuzdan pahalıya) göre sırala
                models.sort((model1, model2) -> {
                    // Önce kredi tipine göre sırala (STANDARD önce)
                    int creditTypeComparison = compareCreditTypes(model1.getCreditType(), model2.getCreditType());
                    if (creditTypeComparison != 0) {
                        return creditTypeComparison;
                    }
                    
                    // Kredi tipi aynıysa, kredi maliyetine göre sırala (küçükten büyüğe)
                    return Integer.compare(
                        model1.getCreditCost() != null ? model1.getCreditCost() : 0, 
                        model2.getCreditCost() != null ? model2.getCreditCost() : 0);
                });
                
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
                            .build()
                        );
                    });
            })
            .doOnSuccess(data -> log.info("AI modelleri başarıyla getirildi: modelCount={}, providerCount={}", 
                data.getModels().size(), data.getProviders() != null ? data.getProviders().size() : 0))
            .doOnError(e -> log.error("AI modelleri getirilirken hata: error={}", e.getMessage()));
    }
    
    // Helper method to compare credit types
    private int compareCreditTypes(String type1, String type2) {
        if (type1 == null) return type2 == null ? 0 : 1;
        if (type2 == null) return -1;
        
        if ("STANDARD".equals(type1) && "ADVANCED".equals(type2)) {
            return -1;
        } else if ("ADVANCED".equals(type1) && "STANDARD".equals(type2)) {
            return 1;
        }
        return 0;
    }
    
    public ModelsData getDefaultModels(String userPlan, Throwable t) {
        log.warn("Varsayılan AI modeller döndürülüyor (fallback): userPlan={}, error={}", userPlan, t.getMessage());
        
        // Basit varsayılan modeller oluştur
        List<AIModel> defaultModels = new ArrayList<>();
        defaultModels.add(AIModel.builder() 
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
            .description("Google AI models")
            .active(true)
            .models(defaultModels)
            .build());
            
        return ModelsData.builder()
            .models(defaultModels)
            .providers(defaultProviders)
            .build();
    }
}
