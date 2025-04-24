package com.craftpilot.userservice.service;

import com.craftpilot.userservice.model.ai.AIModel;
import com.craftpilot.userservice.model.ai.Provider;
import com.craftpilot.userservice.repository.AIModelRepository;
import com.craftpilot.userservice.repository.ProviderRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
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
            // Dosya yolunu belirleme: önce parametre, sonra yapılandırma, en son varsayılan
            String effectivePath = configuredPath != null ? configuredPath : modelsFile;
            
            // Önce dosya sisteminde ara
            File file = new File(effectivePath);
            if (file.exists() && file.isFile()) {
                log.info("Dosya sisteminden modeller yükleniyor: {}", file.getAbsolutePath());
                
                // Dosya formatını kontrol et ve uygun şekilde oku
                if (effectivePath.contains("newmodels.json")) {
                    // newmodels.json formatını direkt olarak liste şeklinde oku
                    return loadNewModelsFormat(file);
                } else {
                    // Eski format için orijinal kodu kullan
                    return loadLegacyModelsFormat(file);
                }
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
                    // Dosya adı kontrolü yap
                    if (effectivePath.contains("newmodels.json")) {
                        return loadNewModelsFormat(inputStream);
                    } else {
                        return loadLegacyModelsFormat(inputStream);
                    }
                }
            }
        } catch (IOException e) {
            log.error("Model JSON dosyası yüklenirken hata oluştu: {}", e.getMessage(), e);
            return Mono.error(e);
        }
    }

    /**
     * newmodels.json formatındaki modelleri yükler
     */
    private Mono<Integer> loadNewModelsFormat(File file) throws IOException {
        List<AIModel> models = objectMapper.readValue(file, new TypeReference<List<AIModel>>() {});
        return processAndSaveModels(models);
    }

    /**
     * newmodels.json formatındaki modelleri input stream'den yükler
     */
    private Mono<Integer> loadNewModelsFormat(InputStream inputStream) throws IOException {
        List<AIModel> models = objectMapper.readValue(inputStream, new TypeReference<List<AIModel>>() {});
        return processAndSaveModels(models);
    }

    /**
     * Eski model formatını yükler
     */
    private Mono<Integer> loadLegacyModelsFormat(File file) throws IOException {
        JsonNode rootNode = objectMapper.readTree(file);
        return processLegacyFormat(rootNode);
    }

    /**
     * Eski model formatını input stream'den yükler
     */
    private Mono<Integer> loadLegacyModelsFormat(InputStream inputStream) throws IOException {
        JsonNode rootNode = objectMapper.readTree(inputStream);
        return processLegacyFormat(rootNode);
    }

    /**
     * Yüklenen modelleri işler ve kaydeder
     */
    private Mono<Integer> processAndSaveModels(List<AIModel> models) {
        // Modelleri sağlayıcılara göre grupla
        Map<String, List<AIModel>> modelsByProvider = new HashMap<>();
        
        for (AIModel model : models) {
            // Modelleri sağlayıcıya göre grupla
            modelsByProvider.computeIfAbsent(model.getProvider(), k -> new ArrayList<>()).add(model);
        }
        
        // Sağlayıcılar oluştur ve kaydet
        List<Mono<Provider>> providerSaveMonos = modelsByProvider.entrySet().stream()
                .map(entry -> {
                    Provider provider = Provider.builder()
                            .name(entry.getKey())
                            .icon(getProviderIcon(entry.getKey()))
                            .description(getProviderDescription(entry.getKey()))
                            .build();
                    return providerRepository.save(provider);
                })
                .collect(Collectors.toList());
        
        // Modelleri kaydet
        List<Mono<AIModel>> modelSaveMonos = models.stream()
                .map(aiModelRepository::save)
                .collect(Collectors.toList());
        
        // Tüm kaydetme işlemlerini birleştir
        return Mono.when(providerSaveMonos)
                .then(Mono.when(modelSaveMonos))
                .thenReturn(models.size());
    }

    /**
     * Eski format için işleme mantığı
     */
    private Mono<Integer> processLegacyFormat(JsonNode rootNode) {
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
                
                // AIModel nesnesini oluştur
                AIModel model = convertJsonNodeToAIModel(modelNode);
                
                models.add(model);
                
                // Sağlayıcıya göre modelleri grupla
                modelsByProvider.computeIfAbsent(model.getProvider(), k -> new ArrayList<>()).add(model);
                
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
                            .build();
                    return providerRepository.save(provider);
                })
                .collect(Collectors.toList());
        // Modelleri kaydet
        List<Mono<AIModel>> modelSaveMonos = models.stream()
                .map(aiModelRepository::save)
                .collect(Collectors.toList());
        // Tüm kaydetme işlemlerini birleştir
        return Mono.when(providerSaveMono)
                .then(Mono.when(modelSaveMonos))
                .thenReturn(models.size());
    }

    /**
     * Model ID'sinden sağlayıcı adını çıkarır
     */
    private String extractProviderFromId(String modelId) {
        if (modelId.contains("/")) {
            return modelId.substring(0, modelId.indexOf("/"));
        }
        return modelId;
    }

    /**
     * Sağlayıcı için ikon adını döndürür
     */
    private String getProviderIcon(String providerKey) {
        Map<String, String> providerIcons = new HashMap<>();
        providerIcons.put("google", "TbBrandGoogle");
        providerIcons.put("Google", "TbBrandGoogle");
        providerIcons.put("openai", "SiOpenai");
        providerIcons.put("OpenAI", "SiOpenai");
        providerIcons.put("anthropic", "TbSettingsAutomation");
        providerIcons.put("Anthropic", "TbSettingsAutomation");
        providerIcons.put("meta-llama", "SiFacebook");
        providerIcons.put("meta", "SiFacebook");
        providerIcons.put("Meta", "SiFacebook");
        providerIcons.put("mistralai", "TbWindmill");
        providerIcons.put("Mistral", "TbWindmill");
        providerIcons.put("Mistral AI", "TbWindmill");
        providerIcons.put("cohere", "TbLayersIntersect");
        providerIcons.put("Cohere", "TbLayersIntersect");
        providerIcons.put("deepseek", "TbBulb");
        providerIcons.put("DeepSeek", "TbBulb");
        providerIcons.put("qwen", "TbCloud");
        providerIcons.put("Qwen", "TbCloud");
        providerIcons.put("x-ai", "TbBrandX");
        providerIcons.put("xAI", "TbBrandX");
        providerIcons.put("microsoft", "SiMicrosoft");
        providerIcons.put("Microsoft", "SiMicrosoft");
        providerIcons.put("perplexity", "TbSearch");
        providerIcons.put("Perplexity", "TbSearch");
        providerIcons.put("AI21", "TbBrain");
        providerIcons.put("NVIDIA", "SiNvidia");
        providerIcons.put("Liquid", "TbWaveSine");
        return providerIcons.getOrDefault(providerKey, "TbBrain");
    }

    /**
     * Modelin kredi maliyetini hesaplar
     */
    private Integer calculateCreditCost(JsonNode modelNode) {
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
        // Bu metot artık kullanılmıyor, ama kod bütünlüğü için bırakıyoruz
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

    private AIModel convertJsonNodeToAIModel(JsonNode modelNode) {
        // JSON'dan modeli çıkar
        String id = modelNode.has("id") ? modelNode.get("id").asText() : "";
        String name = modelNode.has("name") ? modelNode.get("name").asText() : "";
        String modelName = modelNode.has("label") ? modelNode.get("label").asText() : name;
        String provider = modelNode.has("provider") ? modelNode.get("provider").asText() : "";
        Integer contextLength = modelNode.has("context_length") ? modelNode.get("context_length").asInt() : 0;
        
        // Kredi tipini belirle
        String category = modelNode.has("category") ? modelNode.get("category").asText() : "free";
        String creditType = determineCreditType(category);
        
        // Provider bilgilerini çıkarma (yoksa ID'den alınır)
        if (provider.isEmpty()) {
            String providerKey = extractProviderFromId(id);
            provider = getProviderDisplayName(providerKey);
        }
        
        // MaxInputTokens belirleme
        Integer maxInputTokens = determineMaxInputTokens(contextLength);
        
        // Gereken planı belirleme
        String requiredPlan = determineRequiredPlan(modelNode, category);
        
        return AIModel.builder()
                .id(id)                 // Orijinal ID'yi id alanına ata
                .modelId(id)            // Orijinal ID'yi modelId alanına da ata
                .modelName(modelName)
                .provider(provider)
                .maxInputTokens(maxInputTokens)
                .requiredPlan(requiredPlan)
                .creditCost(calculateCreditCost(modelNode))
                .creditType(creditType)  // Kredi tipini ekle
                .category(category)
                .contextLength(contextLength)
                .build();
    }

    /**
     * Model kategorisine göre kredi tipini belirler
     */
    private String determineCreditType(String category) {
        if ("pro".equals(category) || "enterprise".equals(category)) {
            return "ADVANCED";
        }
        return "STANDARD";
    }
}
