package com.craftpilot.userservice.service;

import com.craftpilot.userservice.model.ai.AIModel;
import com.craftpilot.userservice.model.ai.Provider;
import com.craftpilot.userservice.repository.AIModelRepository;
import com.craftpilot.userservice.repository.ProviderRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ModelDataLoader {

    private final AIModelRepository aiModelRepository;
    private final ProviderRepository providerRepository;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;
    
    @Value("${app.models.file:models.json}")
    private String modelsFile;

    /**
     * Yapılandırılabilir JSON dosya yolundan modelleri yükler ve Firestore'a kaydeder
     * @param configuredPath Yapılandırmadan gelen dosya yolu (null ise varsayılan kullanılır)
     * @return Yüklenen model sayısı
     */
    public Mono<Integer> loadModelsFromJson(String configuredPath) {
        try {
            JsonNode rootNode;
            
            // Dosya yolunu belirleme: önce parametre, sonra yapılandırma, en son varsayılan
            String effectivePath = configuredPath != null ? configuredPath : modelsFile;
            
            // Önce dosya sisteminde ara
            File file = new File(effectivePath);
            if (file.exists() && file.isFile()) {
                log.info("Dosya sisteminden modeller yükleniyor: {}", file.getAbsolutePath());
                rootNode = objectMapper.readTree(file);
            } 
            // Sonra kaynaklar içinde ara
            else {
                log.info("Kaynaklardan modeller yükleniyor: {}", effectivePath);
                Resource resource = resourceLoader.getResource("classpath:" + effectivePath);
                if (!resource.exists()) {
                    // Son çare olarak sabit bir ClassPathResource kullan
                    resource = new ClassPathResource("models.json");
                }
                
                try (InputStream inputStream = resource.getInputStream()) {
                    rootNode = objectMapper.readTree(inputStream);
                }
            }
            
            JsonNode dataArray = rootNode.get("data");
            
            if (dataArray == null || !dataArray.isArray()) {
                return Mono.error(new IllegalArgumentException("Geçersiz JSON yapısı. 'data' dizi alanı bulunamadı."));
            }
            
            List<AIModel> models = new ArrayList<>();
            Map<String, List<AIModel>> modelsByProvider = new HashMap<>();
            
            for (JsonNode modelNode : dataArray) {
                // JSON'dan modeli çıkar
                try {
                    String id = modelNode.has("id") ? modelNode.get("id").asText() : "";
                    if (id.isEmpty()) {
                        log.warn("Geçersiz model tanımlayıcısı, atlanıyor");
                        continue;
                    }
                    
                    String name = modelNode.has("name") ? modelNode.get("name").asText() : "";
                    String description = modelNode.has("description") ? modelNode.get("description").asText() : "";
                    
                    // Provider bilgilerini ayıklaan çıkarma
                    String providerKey = extractProviderFromId(id);
                    String providerName = getProviderDisplayName(providerKey);
                    
                    // Bağlam uzunluğu
                    Integer contextLength = modelNode.has("context_length") ? modelNode.get("context_length").asInt(0) : 0;
                    
                    // Oluşturulma tarihi
                    Long createdTimestamp = modelNode.has("created") ? modelNode.get("created").asLong(0) : 0;
                    
                    // Model kredit maliyetini belirle
                    Integer creditCost = calculateCreditCost(modelNode);
                    
                    // Model kategorisini belirle
                    String category = determineCategory(modelNode);
                    
                    // Model için gereken planı belirle
                    String requiredPlan = determineRequiredPlan(modelNode, category);
                    
                    // Model popülerliğini belirle
                    Boolean popular = isPremiumModel(modelNode) || isPopularModel(id);
                    
                    // Token limitlerini belirle
                    Integer maxTokens = determineMaxTokens(contextLength);
                    Integer maxInputTokens = determineMaxInputTokens(contextLength);
                    
                    // Mimari bilgilerini çıkar
                    AIModel.Architecture architecture = extractArchitecture(modelNode);
                    
                    // Fiyatlandırma bilgilerini çıkar
                    AIModel.Pricing pricing = extractPricing(modelNode);
                    
                    // Provider özel bilgilerini çıkar
                    AIModel.TopProvider topProvider = extractTopProvider(modelNode);
                    
                    // AIModel nesnesini oluştur
                    AIModel model = AIModel.builder()
                            .id(id)
                            .value(id)
                            .label(name)
                            .description(description)
                            .provider(providerName)
                            .providerIcon(getProviderIcon(providerKey))
                            .maxTokens(maxTokens)
                            .maxInputTokens(maxInputTokens)
                            .requiredPlan(requiredPlan)
                            .creditCost(creditCost)
                            .category(category)
                            .popular(popular)
                            .contextLength(contextLength)
                            .created(createdTimestamp)
                            .architecture(architecture)
                            .pricing(pricing)
                            .topProvider(topProvider)
                            .perRequestLimits(null)
                            .build();
                    
                    models.add(model);
                    
                    // Sağlayıcıya göre modelleri grupla
                    modelsByProvider.computeIfAbsent(providerName, k -> new ArrayList<>()).add(model);
                    
                } catch (Exception e) {
                    log.error("Model işlenirken hata oluştu: {}", e.getMessage(), e);
                }
            }
            
            // Sağlayıcılar oluştur ve kaydet
            List<Mono<Provider>> providerSaveMono = modelsByProvider.entrySet().stream()
                    .map(entry -> {
                        Provider provider = Provider.builder()
                                .name(entry.getKey())
                                .icon(getProviderIcon(entry.getKey()))
                                .description(getProviderDescription(entry.getKey()))
                                .build();ntry.getValue())
                        return providerRepository.save(provider);
                    })  return providerRepository.save(provider);
                    .collect(Collectors.toList());
                    .collect(Collectors.toList());
            // Modelleri kaydet
            List<Mono<AIModel>> modelSaveMonos = models.stream()
                    .map(aiModelRepository::save)models.stream()
                    .collect(Collectors.toList());
                    .collect(Collectors.toList());
            // Tüm kaydetme işlemlerini birleştir
            return Mono.when(providerSaveMono)tir
                    .then(Mono.when(modelSaveMonos))
                    .thenReturn(models.size());nos))
                    .thenReturn(models.size());
        } catch (IOException e) {
            log.error("Model JSON dosyası yüklenirken hata oluştu: {}", e.getMessage(), e);
            return Mono.error(e); dosyası yüklenirken hata oluştu: {}", e.getMessage(), e);
        }   return Mono.error(e);
    }   }
    }
    /**
     * Model ID'sinden sağlayıcı adını çıkarır
     */Model ID'sinden sağlayıcı adını çıkarır
    private String extractProviderFromId(String modelId) {
        if (modelId.contains("/")) {omId(String modelId) {
            return modelId.substring(0, modelId.indexOf("/"));
        }   return modelId.substring(0, modelId.indexOf("/"));
        return modelId;
    }   return modelId;
    }
    // Yeni yardımcı metotlar
    private AIModel.Architecture extractArchitecture(JsonNode modelNode) {
        AIModel.Architecture architecture = new AIModel.Architecture();) {
        AIModel.Architecture architecture = new AIModel.Architecture();
        if (modelNode.has("architecture")) {
            JsonNode archNode = modelNode.get("architecture");
            JsonNode archNode = modelNode.get("architecture");
            if (archNode.has("modality")) {
                architecture.setModality(archNode.get("modality").asText());
            }   architecture.setModality(archNode.get("modality").asText());
            }
            List<String> inputModalities = new ArrayList<>();
            if (archNode.has("input_modalities") && archNode.get("input_modalities").isArray()) {
                for (JsonNode modalityNode : archNode.get("input_modalities")) {es").isArray()) {
                    inputModalities.add(modalityNode.asText());t_modalities")) {
                }   inputModalities.add(modalityNode.asText());
            }   }
            architecture.setInputModalities(inputModalities);
            architecture.setInputModalities(inputModalities);
            List<String> outputModalities = new ArrayList<>();
            if (archNode.has("output_modalities") && archNode.get("output_modalities").isArray()) {
                for (JsonNode modalityNode : archNode.get("output_modalities")) {ies").isArray()) {
                    outputModalities.add(modalityNode.asText());t_modalities")) {
                }   outputModalities.add(modalityNode.asText());
            }   }
            architecture.setOutputModalities(outputModalities);
            architecture.setOutputModalities(outputModalities);
            if (archNode.has("tokenizer")) {
                architecture.setTokenizer(archNode.get("tokenizer").asText());
            }   architecture.setTokenizer(archNode.get("tokenizer").asText());
            }
            if (archNode.has("instruct_type")) {
                if (archNode.get("instruct_type").isNull()) {
                    architecture.setInstructType(null);l()) {
                } else {itecture.setInstructType(null);
                    architecture.setInstructType(archNode.get("instruct_type").asText());
                }   architecture.setInstructType(archNode.get("instruct_type").asText());
            }   }
        }   }
        }
        return architecture;
    }   return architecture;
    }
    private AIModel.Pricing extractPricing(JsonNode modelNode) {
        AIModel.Pricing pricing = new AIModel.Pricing();lNode) {
        AIModel.Pricing pricing = new AIModel.Pricing();
        if (modelNode.has("pricing")) {
            JsonNode pricingNode = modelNode.get("pricing");
            JsonNode pricingNode = modelNode.get("pricing");
            if (pricingNode.has("prompt")) {
                pricing.setPrompt(pricingNode.get("prompt").asText());
            }   pricing.setPrompt(pricingNode.get("prompt").asText());
            }
            if (pricingNode.has("completion")) {
                pricing.setCompletion(pricingNode.get("completion").asText());
            }   pricing.setCompletion(pricingNode.get("completion").asText());
            }
            if (pricingNode.has("request")) {
                pricing.setRequest(pricingNode.get("request").asText());
            }   pricing.setRequest(pricingNode.get("request").asText());
            }
            if (pricingNode.has("image")) {
                pricing.setImage(pricingNode.get("image").asText());
            }   pricing.setImage(pricingNode.get("image").asText());
            }
            if (pricingNode.has("web_search")) {
                pricing.setWebSearch(pricingNode.get("web_search").asText());
            }   pricing.setWebSearch(pricingNode.get("web_search").asText());
            }
            if (pricingNode.has("internal_reasoning")) {
                pricing.setInternalReasoning(pricingNode.get("internal_reasoning").asText());
            }   pricing.setInternalReasoning(pricingNode.get("internal_reasoning").asText());
            }
            if (pricingNode.has("input_cache_read")) {
                pricing.setInputCacheRead(pricingNode.get("input_cache_read").asText());
            }   pricing.setInputCacheRead(pricingNode.get("input_cache_read").asText());
            }
            if (pricingNode.has("input_cache_write")) {
                pricing.setInputCacheWrite(pricingNode.get("input_cache_write").asText());
            }   pricing.setInputCacheWrite(pricingNode.get("input_cache_write").asText());
        }   }
        }
        return pricing;
    }   return pricing;
    }
    private AIModel.TopProvider extractTopProvider(JsonNode modelNode) {
        AIModel.TopProvider topProvider = new AIModel.TopProvider();e) {
        AIModel.TopProvider topProvider = new AIModel.TopProvider();
        if (modelNode.has("top_provider")) {
            JsonNode providerNode = modelNode.get("top_provider");
            JsonNode providerNode = modelNode.get("top_provider");
            if (providerNode.has("context_length")) {
                topProvider.setContextLength(providerNode.get("context_length").asInt());
            }   topProvider.setContextLength(providerNode.get("context_length").asInt());
            }
            if (providerNode.has("max_completion_tokens")) {
                if (providerNode.get("max_completion_tokens").isNull()) {
                    topProvider.setMaxCompletionTokens(null);.isNull()) {
                } else {rovider.setMaxCompletionTokens(null);
                    topProvider.setMaxCompletionTokens(providerNode.get("max_completion_tokens").asInt());
                }   topProvider.setMaxCompletionTokens(providerNode.get("max_completion_tokens").asInt());
            }   }
            }
            if (providerNode.has("is_moderated")) {
                topProvider.setIsModerated(providerNode.get("is_moderated").asBoolean());
            }   topProvider.setIsModerated(providerNode.get("is_moderated").asBoolean());
        }   }
        }
        return topProvider;
    }   return topProvider;
    }
    /**
     * Sağlayıcı için ikon adını döndürür
     */Sağlayıcı için ikon adını döndürür
    private String getProviderIcon(String providerKey) {
        Map<String, String> providerIcons = new HashMap<>();
        providerIcons.put("google", "TbBrandGoogle");ap<>();
        providerIcons.put("openai", "SiOpenai");le");
        providerIcons.put("anthropic", "TbSettingsAutomation");
        providerIcons.put("meta-llama", "SiFacebook");mation");
        providerIcons.put("meta", "SiFacebook");ook");
        providerIcons.put("mistralai", "TbWindmill");
        providerIcons.put("cohere", "TbLayersIntersect");
        providerIcons.put("deepseek", "TbBulb");ersect");
        providerIcons.put("qwen", "TbCloud");");
        providerIcons.put("x-ai", "TbBrandX");
        providerIcons.put("microsoft", "SiMicrosoft");
        providerIcons.put("perplexity", "TbSearch"););
        providerIcons.put("perplexity", "TbSearch");
        return providerIcons.getOrDefault(providerKey, "TbBrain");
    }   return providerIcons.getOrDefault(providerKey, "TbBrain");
    }
    /**
     * Modelin kredi maliyetini hesaplar
     */Modelin kredi maliyetini hesaplar
    private Integer calculateCreditCost(JsonNode modelNode) {
        int baseCost = 1; // Temel maliyetonNode modelNode) {
        int baseCost = 1; // Temel maliyet
        // Context length büyükse maliyet artar
        int contextLength = modelNode.has("context_length") ? modelNode.get("context_length").asInt() : 0;
        if (contextLength > 100000) baseCost += 2;_length") ? modelNode.get("context_length").asInt() : 0;
        else if (contextLength > 32000) baseCost += 1;
        else if (contextLength > 32000) baseCost += 1;
        // Premium modeller daha pahalı
        if (isPremiumModel(modelNode)) {
            baseCost += 2;(modelNode)) {
        }   baseCost += 2;
        }
        // Bazı özel modellere farklı kredi maliyeti atama
        String id = modelNode.has("id") ? modelNode.get("id").asText() : "";
        if (id.contains("gpt-4") || id.contains("claude-3") || sText() : "";
            id.contains("gemini") || id.contains("mistral-large") ||
            id.contains("o1") || id.contains("o3") || id.contains("o4")) {
            baseCost += 1;1") || id.contains("o3") || id.contains("o4")) {
        }   baseCost += 1;
        }
        return baseCost;
    }   return baseCost;
    }
    /**
     * Model kategorisini belirler (basic, standard, premium)
     */Model kategorisini belirler (basic, standard, premium)
    private String determineCategory(JsonNode modelNode) {
        String id = modelNode.has("id") ? modelNode.get("id").asText() : "";
        String id = modelNode.has("id") ? modelNode.get("id").asText() : "";
        // Premium modeller
        if (isPremiumModel(modelNode)) {
            return "premium";delNode)) {
        }   return "premium";
        }
        // Standard modeller
        if (isStandardModel(modelNode)) {
            return "standard";delNode)) {
        }   return "standard";
        }
        // Geri kalanlar basic
        return "basic";r basic
    }   return "basic";
    }
    private boolean isPremiumModel(JsonNode modelNode) {
        String id = modelNode.has("id") ? modelNode.get("id").asText() : "";
        String id = modelNode.has("id") ? modelNode.get("id").asText() : "";
        // Premium modeller
        return id.contains("gpt-4") || 
               id.contains("claude-3") || 
               id.contains("o1") || ") || 
               id.contains("o3") || 
               id.contains("o4") || 
               id.contains("gemini-pro") || 
               id.contains("mistral-large") ||
               id.contains("llama-4");rge") ||
    }          id.contains("llama-4");
    }
    private boolean isStandardModel(JsonNode modelNode) {
        String id = modelNode.has("id") ? modelNode.get("id").asText() : "";
        int contextLength = modelNode.has("context_length") ? modelNode.get("context_length").asInt() : 0;
        int contextLength = modelNode.has("context_length") ? modelNode.get("context_length").asInt() : 0;
        // Standard modeller
        return contextLength > 16000 || 
               id.contains("gemini") || 
               id.contains("gpt-3.5") || 
               id.contains("mistral") || 
               id.contains("llama-3") ||
               id.contains("codestral") ||
               id.contains("claude-2"); ||
    }          id.contains("claude-2");
    }
    /**
     * Model için gereken planı belirler
     */Model için gereken planı belirler
    private String determineRequiredPlan(JsonNode modelNode, String category) {
        if ("premium".equals(category)) {JsonNode modelNode, String category) {
            return "premium";category)) {
        } else if ("standard".equals(category)) {
            return "pro";ard".equals(category)) {
        } else {rn "pro";
            return "free";
        }   return "free";
    }   }
    }
    /**
     * Modelin maksimum token limitini belirler
     */Modelin maksimum token limitini belirler
    private Integer determineMaxTokens(Integer contextLength) {
        if (contextLength == 0) return 4000;er contextLength) {
        return Math.min(contextLength / 2, 8000); // Context'in yarısı kadar, maksimum 8000
    }   return Math.min(contextLength / 2, 8000); // Context'in yarısı kadar, maksimum 8000
    }
    /**
     * Modelin maksimum girdi token limitini belirler
     */Modelin maksimum girdi token limitini belirler
    private Integer determineMaxInputTokens(Integer contextLength) {
        if (contextLength == 0) return 3000;Integer contextLength) {
        return Math.min(contextLength * 3 / 4, 6000); // Context'in 3/4'ü kadar, maksimum 6000
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
