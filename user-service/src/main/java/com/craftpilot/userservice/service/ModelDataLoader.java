package com.craftpilot.userservice.service;

import com.craftpilot.userservice.model.ai.AIModel;
import com.craftpilot.userservice.model.ai.Provider;
import com.craftpilot.userservice.repository.AIModelRepository;
import com.craftpilot.userservice.repository.ProviderRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ModelDataLoader {

    private final AIModelRepository aiModelRepository;
    private final ProviderRepository providerRepository;
    private final ObjectMapper objectMapper;

    /**
     * Models.json dosyasından modelleri yükler ve Firestore'a kaydeder
     * @param jsonFilePath models.json dosya yolu
     * @return Yüklenen model sayısı
     */
    public Mono<Integer> loadModelsFromJson(String jsonFilePath) {
        try {
            File file = new File(jsonFilePath);
            JsonNode rootNode = objectMapper.readTree(file);
            JsonNode dataArray = rootNode.get("data");
            
            if (dataArray == null || !dataArray.isArray()) {
                return Mono.error(new IllegalArgumentException("Geçersiz JSON yapısı. 'data' dizi alanı bulunamadı."));
            }
            
            List<AIModel> models = new ArrayList<>();
            Map<String, List<AIModel>> modelsByProvider = new HashMap<>();
            
            for (JsonNode modelNode : dataArray) {
                // JSON'dan modeli çıkar
                try {
                    // Temel özellikler
                    String id = modelNode.has("id") ? modelNode.get("id").asText() : null;
                    
                    // ID yoksa bu modeli atla
                    if (id == null || id.isEmpty()) {
                        continue;
                    }
                    
                    String name = modelNode.has("name") ? modelNode.get("name").asText() : "";
                    String description = modelNode.has("description") ? modelNode.get("description").asText() : "";
                    Integer contextLength = modelNode.has("context_length") ? modelNode.get("context_length").asInt() : 0;
                    
                    // Sağlayıcı bilgisini çıkar
                    String providerName = "Diğer";
                    String providerIcon = "TbBrain";
                    
                    if (id.contains("/")) {
                        String[] parts = id.split("/", 2);
                        providerName = getProviderDisplayName(parts[0]);
                        providerIcon = getProviderIcon(parts[0]);
                    }
                    
                    // Model kategorisini belirle
                    String category = determineCategory(modelNode);
                    
                    // Kredi maliyetini belirle
                    Integer creditCost = calculateCreditCost(modelNode);
                    
                    // Mimari bilgisini al
                    String architecture = "Bilinmiyor";
                    if (modelNode.has("architecture") && modelNode.get("architecture").has("instruct_type")) {
                        JsonNode architectureNode = modelNode.get("architecture").get("instruct_type");
                        if (!architectureNode.isNull()) {
                            architecture = architectureNode.asText();
                        }
                    }
                    
                    // Gerekli planı belirle
                    String requiredPlan = determineRequiredPlan(modelNode, category);
                    
                    // Model popülerliğini belirle
                    Boolean popular = isPremiumModel(modelNode) || isPopularModel(id);
                    
                    // Token limitlerini belirle
                    Integer maxTokens = determineMaxTokens(contextLength);
                    Integer maxInputTokens = determineMaxInputTokens(contextLength);
                    
                    // AIModel nesnesini oluştur
                    AIModel model = AIModel.builder()
                            .id(id)
                            .value(id)
                            .label(name)
                            .description(description)
                            .badge(category.equals("premium") ? "Yeni" : null)
                            .popular(popular)
                            .provider(providerName)
                            .providerIcon(providerIcon)
                            .maxTokens(maxTokens)
                            .maxInputTokens(maxInputTokens)
                            .requiredPlan(requiredPlan)
                            .creditCost(creditCost)
                            .category(category)
                            .architecture(architecture)
                            .contextLength(contextLength)
                            .build();
                    
                    models.add(model);
                    
                    // Sağlayıcı bazında modelleri gruplandır
                    modelsByProvider.computeIfAbsent(providerName, k -> new ArrayList<>()).add(model);
                    
                } catch (Exception e) {
                    log.error("Model işlenirken hata oluştu: {}", e.getMessage());
                }
            }
            
            log.info("Toplam {} model işlendi.", models.size());
            
            // Önce tüm modelleri kaydet
            return Flux.fromIterable(models)
                    .flatMap(aiModelRepository::save)
                    .collectList()
                    .flatMapMany(savedModels -> {
                        // Ardından sağlayıcıları kaydet
                        List<Provider> providers = modelsByProvider.entrySet().stream()
                                .map(entry -> Provider.builder()
                                        .name(entry.getKey())
                                        .icon(getProviderIcon(entry.getKey()))
                                        .description(getProviderDescription(entry.getKey()))
                                        .models(entry.getValue())
                                        .build())
                                .collect(Collectors.toList());
                        
                        return Flux.fromIterable(providers).flatMap(providerRepository::save);
                    })
                    .collectList()
                    .map(List::size);
            
        } catch (IOException e) {
            log.error("JSON dosyası yüklenirken hata: {}", e.getMessage());
            return Mono.error(e);
        }
    }
    
    /**
     * Modelin kredi maliyetini hesaplar
     */
    private Integer calculateCreditCost(JsonNode modelNode) {
        // Kredi maliyeti hesaplama mantığı:
        // 1. Context length'e göre
        // 2. Premium modeller daha pahalı
        // 3. Popüler/yeni modeller daha pahalı
        
        int baseCost = 1; // Temel maliyet
        
        // Context length büyükse maliyet artar
        int contextLength = modelNode.has("context_length") ? modelNode.get("context_length").asInt() : 0;
        if (contextLength > 100000) baseCost += 2;
        else if (contextLength > 32000) baseCost += 1;
        
        // Premium modeller daha pahalı
        if (isPremiumModel(modelNode)) {
            baseCost += 2;
        }
        
        // Bazı özel modellere farklı kredi maliyeti atama
        String id = modelNode.has("id") ? modelNode.get("id").asText() : "";
        if (id.contains("gpt-4") || id.contains("claude-3") || 
            id.contains("gemini") || id.contains("mistral-large") ||
            id.contains("o1") || id.contains("o3") || id.contains("o4")) {
            baseCost += 1;
        }
        
        return baseCost;
    }
    
    /**
     * Model kategorisini belirler (basic, standard, premium)
     */
    private String determineCategory(JsonNode modelNode) {
        String id = modelNode.has("id") ? modelNode.get("id").asText() : "";
        
        // Premium modeller
        if (isPremiumModel(modelNode)) {
            return "premium";
        }
        
        // Standard modeller
        if (isStandardModel(modelNode)) {
            return "standard";
        }
        
        // Geri kalanlar basic
        return "basic";
    }
    
    private boolean isPremiumModel(JsonNode modelNode) {
        String id = modelNode.has("id") ? modelNode.get("id").asText() : "";
        
        // Premium modeller
        return id.contains("gpt-4") || 
               id.contains("claude-3") || 
               id.contains("o1") || 
               id.contains("o3") || 
               id.contains("o4") ||
               id.contains("gemini-pro") || 
               id.contains("mistral-large") ||
               id.contains("llama-4");
    }
    
    private boolean isStandardModel(JsonNode modelNode) {
        String id = modelNode.has("id") ? modelNode.get("id").asText() : "";
        int contextLength = modelNode.has("context_length") ? modelNode.get("context_length").asInt() : 0;
        
        // Standard modeller
        return contextLength > 16000 || 
               id.contains("gemini") || 
               id.contains("gpt-3.5") || 
               id.contains("mistral") ||
               id.contains("llama-3") ||
               id.contains("codestral") ||
               id.contains("claude-2");
    }
    
    /**
     * Model için gereken planı belirler
     */
    private String determineRequiredPlan(JsonNode modelNode, String category) {
        if ("premium".equals(category)) {
            return "premium";
        } else if ("standard".equals(category)) {
            return "pro";
        } else {
            return "free";
        }
    }
    
    /**
     * Modelin maksimum token limitini belirler
     */
    private Integer determineMaxTokens(Integer contextLength) {
        if (contextLength == 0) return 4000;
        return Math.min(contextLength / 2, 8000); // Context'in yarısı kadar, maksimum 8000
    }
    
    /**
     * Modelin maksimum girdi token limitini belirler
     */
    private Integer determineMaxInputTokens(Integer contextLength) {
        if (contextLength == 0) return 3000;
        return Math.min(contextLength * 3 / 4, 6000); // Context'in 3/4'ü kadar, maksimum 6000
    }
    
    /**
     * Popüler modelleri belirle
     */
    private boolean isPopularModel(String id) {
        Set<String> popularModels = new HashSet<>(Arrays.asList(
            "google/gemini-pro-1.5",
            "openai/gpt-4o",
            "anthropic/claude-3-opus",
            "mistralai/mistral-large",
            "meta-llama/llama-3-70b-instruct",
            "anthropic/claude-3-sonnet",
            "google/gemini-flash-1.5"
        ));
        
        return popularModels.contains(id);
    }
    
    /**
     * Sağlayıcıların görünen adlarını döndürür
     */
    private String getProviderDisplayName(String providerKey) {
        Map<String, String> providerNames = new HashMap<>();
        providerNames.put("google", "Google");
        providerNames.put("openai", "OpenAI");
        providerNames.put("anthropic", "Anthropic");
        providerNames.put("meta-llama", "Meta");
        providerNames.put("mistralai", "Mistral AI");
        providerNames.put("cohere", "Cohere");
        providerNames.put("deepseek", "DeepSeek");
        providerNames.put("qwen", "Qwen");
        providerNames.put("x-ai", "xAI");
        providerNames.put("microsoft", "Microsoft");
        providerNames.put("perplexity", "Perplexity");
        
        return providerNames.getOrDefault(providerKey, providerKey);
    }
    
    /**
     * Sağlayıcı ikonlarını döndürür
     */
    private String getProviderIcon(String providerKey) {
        Map<String, String> providerIcons = new HashMap<>();
        providerIcons.put("google", "TbBrandGoogle");
        providerIcons.put("openai", "SiOpenai");
        providerIcons.put("anthropic", "TbSettingsAutomation");
        providerIcons.put("meta-llama", "SiFacebook");
        providerIcons.put("meta", "SiFacebook");
        providerIcons.put("mistralai", "TbWindmill");
        providerIcons.put("cohere", "TbLayersIntersect");
        providerIcons.put("deepseek", "TbBulb");
        providerIcons.put("qwen", "TbCloud");
        providerIcons.put("x-ai", "TbBrandX");
        providerIcons.put("microsoft", "SiMicrosoft");
        providerIcons.put("perplexity", "TbSearch");
        
        return providerIcons.getOrDefault(providerKey, "TbBrain");
    }
    
    /**
     * Sağlayıcı açıklamalarını döndürür
     */
    private String getProviderDescription(String providerKey) {
        Map<String, String> providerDescriptions = new HashMap<>();
        providerDescriptions.put("Google", "Google'ın Gemini serisi modelleri");
        providerDescriptions.put("OpenAI", "GPT serisi ve diğer OpenAI modelleri");
        providerDescriptions.put("Anthropic", "Claude serisi modeller");
        providerDescriptions.put("Meta", "Llama serisi açık kaynak modeller");
        providerDescriptions.put("Mistral AI", "Mistral ve türev modeller");
        providerDescriptions.put("Cohere", "Cohere serisi modeller");
        providerDescriptions.put("DeepSeek", "DeepSeek serisi modeller");
        providerDescriptions.put("Qwen", "Qwen serisi modeller");
        providerDescriptions.put("xAI", "Grok serisi modeller");
        providerDescriptions.put("Microsoft", "Microsoft AI modelleri");
        providerDescriptions.put("Perplexity", "Perplexity AI modelleri");
        
        return providerDescriptions.getOrDefault(providerKey, providerKey + " modelleri");
    }
}
