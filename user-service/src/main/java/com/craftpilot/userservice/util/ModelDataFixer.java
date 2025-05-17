package com.craftpilot.userservice.util;

import com.craftpilot.userservice.model.ai.AIModel;
import com.craftpilot.userservice.model.ai.Provider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Component
public class ModelDataFixer {
    
    private final ObjectMapper objectMapper;
    
    public ModelDataFixer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    /**
     * JSON model verilerini doğrular ve sabitler
     * 
     * @param jsonContent JSON içeriği
     * @return Doğrulanmış ve sabitlenmiş model ve sağlayıcı listesi
     */
    public Map<String, Object> validateAndFixModelData(String jsonContent) {
        try {
            List<AIModel> fixedModels = new ArrayList<>();
            Map<String, Provider> providers = new HashMap<>();
            
            JsonNode rootNode = objectMapper.readTree(jsonContent);
            
            for (JsonNode modelNode : rootNode) {
                // Özel model formatını kontrol et
                if (modelNode.has("provider") && modelNode.has("models") && modelNode.has("contextLength")) {
                    // Bu, özel bir model formatı - models array'i içerir
                    processProviderModelsFormat(modelNode, fixedModels, providers);
                } else if (modelNode.has("modelId") && modelNode.has("modelName") && modelNode.has("provider")) {
                    // Standart model formatı
                    AIModel model = processStandardModelFormat(modelNode);
                    if (model != null) {
                        fixedModels.add(model);
                        
                        // Provider bilgisini sakla
                        String providerName = modelNode.get("provider").asText();
                        if (!providers.containsKey(providerName)) {
                            Provider provider = Provider.builder()
                                    .name(providerName)
                                    .build();
                            providers.put(providerName, provider);
                        }
                    }
                }
            }
            
            log.info("Doğrulama tamamlandı: {} model ve {} provider bulundu", 
                    fixedModels.size(), providers.size());
            
            Map<String, Object> result = new HashMap<>();
            result.put("models", fixedModels);
            result.put("providers", new ArrayList<>(providers.values()));
            
            return result;
            
        } catch (JsonProcessingException e) {
            log.error("JSON parse hatası: {}", e.getMessage());
            throw new RuntimeException("JSON parsing failed", e);
        }
    }
    
    private void processProviderModelsFormat(JsonNode modelNode, List<AIModel> models, Map<String, Provider> providers) {
        String providerName = modelNode.get("provider").asText();
        int contextLength = modelNode.get("contextLength").asInt();
        
        // Provider bilgisini sakla
        if (!providers.containsKey(providerName)) {
            Provider provider = Provider.builder()
                    .name(providerName)
                    .build();
            providers.put(providerName, provider);
        }
        
        // Her bir alt model için AIModel oluştur
        if (modelNode.has("models") && modelNode.get("models").isArray()) {
            for (JsonNode subModel : modelNode.get("models")) {
                String name = subModel.has("name") ? subModel.get("name").asText() : null;
                String modelId = subModel.has("modelId") ? subModel.get("modelId").asText() : null;
                Integer maxTokens = subModel.has("maxTokens") ? subModel.get("maxTokens").asInt() : null;
                
                if (modelId != null && name != null) {
                    AIModel model = AIModel.builder()
                            .modelId(modelId)
                            .modelName(name)
                            .provider(providerName)
                            .contextLength(contextLength)
                            .maxInputTokens(maxTokens != null ? maxTokens : 8000)
                            .requiredPlan("FREE")
                            .creditCost(1)
                            .creditType("STANDARD")
                            .category("FREE")
                            .build();
                    
                    models.add(model);
                }
            }
        }
    }
    
    private AIModel processStandardModelFormat(JsonNode modelNode) {
        String modelId = modelNode.has("modelId") ? modelNode.get("modelId").asText() : null;
        String modelName = modelNode.has("modelName") ? modelNode.get("modelName").asText() : null;
        String provider = modelNode.has("provider") ? modelNode.get("provider").asText() : null;
        
        // Model ID, isim veya provider boşsa bu modeli atla
        if (modelId == null || modelId.isEmpty() || modelName == null || provider == null) {
            log.warn("Geçersiz model verisi, atlanıyor: {}", modelNode);
            return null;
        }
        
        return AIModel.builder()
                .modelId(modelId)
                .modelName(modelName)
                .provider(provider)
                .maxInputTokens(modelNode.has("maxInputTokens") ? modelNode.get("maxInputTokens").asInt() : 8000)
                .requiredPlan(modelNode.has("requiredPlan") ? modelNode.get("requiredPlan").asText() : "FREE")
                .creditCost(modelNode.has("creditCost") ? modelNode.get("creditCost").asInt() : 1)
                .creditType(modelNode.has("creditType") ? modelNode.get("creditType").asText() : "STANDARD")
                .category(modelNode.has("category") ? modelNode.get("category").asText() : "FREE")
                .contextLength(modelNode.has("contextLength") ? modelNode.get("contextLength").asInt() : 8000)
                .build();
    }
}
