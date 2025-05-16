package com.craftpilot.userservice.service;

import com.craftpilot.userservice.model.ai.AIModel;
import com.craftpilot.userservice.model.ai.Provider;
import com.craftpilot.userservice.repository.AIModelRepository;
import com.craftpilot.userservice.repository.ProviderRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
@RequiredArgsConstructor
public class ModelDataLoader {

    private final ProviderRepository providerRepository;
    private final AIModelRepository modelRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.models.file:newmodels.json}")
    private String modelsFile;
    
    @Value("${app.load-models:false}")
    private boolean loadModelsEnabled;

    /**
     * Yapılandırılabilir JSON dosya yolundan modelleri yükler ve MongoDB'ye
     * kaydeder - sadece app.load-models=true ise PostConstruct sırasında çalışır
     */
    @PostConstruct
    public void loadModelsOnStartup() {
        if (!loadModelsEnabled) {
            log.info("Otomatik model yükleme devre dışı bırakılmış (app.load-models=false)");
            return;
        }
        
        log.info("Başlangıçta model yükleme etkin. '{}' dosyasından modeller yükleniyor", modelsFile);
        
        loadModels(modelsFile)
                .subscribe(
                        count -> log.info("{} adet model başarıyla yüklendi", count),
                        error -> log.error("Model yükleme sırasında hata oluştu: {}", error.getMessage(), error));
    }

    public Mono<Integer> loadModels(String configuredPath) {
        String path = configuredPath != null ? configuredPath : modelsFile;
        log.info("Modeller {} yolundan yükleniyor", path);

        try {
            // Önce classpath'ten yüklemeyi dene
            String resourcePath = path;
            if (!path.startsWith("classpath:")) {
                resourcePath = "classpath:" + path;
            }
            
            org.springframework.core.io.Resource resource = 
                new org.springframework.core.io.ClassPathResource(path.replace("classpath:", ""));
            
            if (!resource.exists()) {
                // Eğer classpath'te yoksa, dosya sisteminden yüklemeyi dene
                Path filePath = Paths.get(path.replace("classpath:", ""));
                
                // Dosyanın varlığını kontrol et
                if (!Files.exists(filePath)) {
                    log.error("Model yükleme başarısız: Dosya bulunamadı: {}", filePath.toAbsolutePath());
                    return Mono.error(new IOException("Model dosyası bulunamadı: " + filePath.toAbsolutePath()));
                }
                
                log.info("Modeller dosya sisteminden yükleniyor: {}", filePath.toAbsolutePath());
                String jsonContent = Files.readString(filePath);
                return processJsonContent(jsonContent);
            } else {
                // Classpath'ten yükle
                log.info("Modeller classpath kaynağından yükleniyor: {}", resource.getURL());
                try (InputStream is = resource.getInputStream()) {
                    String jsonContent = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    return processJsonContent(jsonContent);
                }
            }
        } catch (IOException e) {
            log.error("JSON model verisi yüklenirken hata: {}", e.getMessage(), e);
            return Mono.error(e);
        }
    }
    
    private Mono<Integer> processJsonContent(String jsonContent) {
        try {
            log.debug("JSON içeriği işleniyor");
            List<Object> jsonObjects = objectMapper.readValue(
                    jsonContent,
                    new TypeReference<List<Object>>() {});
            
            AtomicInteger modelCount = new AtomicInteger(0);
            List<Mono<Void>> processTasks = new ArrayList<>();
            
            for (Object obj : jsonObjects) {
                try {
                    // Her bir JSON objesini ayrı ayrı değerlendir
                    if (obj instanceof java.util.Map) {
                        java.util.Map<String, Object> itemMap = (java.util.Map<String, Object>) obj;
                        
                        // Meta provider gibi özel formatlar için
                        if (itemMap.containsKey("provider") && itemMap.containsKey("models") && itemMap.get("models") instanceof List) {
                            String providerName = (String) itemMap.get("provider");
                            Integer contextLength = itemMap.containsKey("contextLength") ? 
                                                    ((Number)itemMap.get("contextLength")).intValue() : null;
                            
                            Provider provider = Provider.builder()
                                    .name(providerName)
                                    .build();
                            
                            // Provider kaydetme
                            Mono<Void> providerTask = providerRepository.save(provider)
                                    .flatMap(savedProvider -> {
                                        List<Object> modelsList = (List<Object>) itemMap.get("models");
                                        List<Mono<AIModel>> modelSaveOps = new ArrayList<>();
                                        
                                        for (Object modelObj : modelsList) {
                                            if (modelObj instanceof java.util.Map) {
                                                java.util.Map<String, Object> modelMap = (java.util.Map<String, Object>) modelObj;
                                                
                                                AIModel model = AIModel.builder()
                                                    .id(String.valueOf(modelMap.get("modelId")))
                                                    .modelId(String.valueOf(modelMap.get("modelId")))
                                                    .modelName(String.valueOf(modelMap.get("name")))
                                                    .provider(savedProvider.getName())
                                                    .maxInputTokens(modelMap.containsKey("maxTokens") ? 
                                                                   ((Number)modelMap.get("maxTokens")).intValue() : 8000)
                                                    .contextLength(contextLength != null ? contextLength : 131072)
                                                    .build();
                                                
                                                modelSaveOps.add(modelRepository.save(model)
                                                    .doOnSuccess(m -> modelCount.incrementAndGet()));
                                            }
                                        }
                                        
                                        return Flux.concat(modelSaveOps).then();
                                    })
                                    .then();
                            
                            processTasks.add(providerTask);
                        } 
                        // Normal model objeleri için
                        else if (itemMap.containsKey("id") && itemMap.containsKey("modelId")) {
                            AIModel model = objectMapper.convertValue(obj, AIModel.class);
                            
                            // Eğer provider yoksa varsayılan bir değer belirle
                            if (model.getProvider() == null && itemMap.containsKey("provider")) {
                                model.setProvider((String) itemMap.get("provider"));
                            }
                            
                            // Provider'ı kaydet
                            Provider provider = Provider.builder()
                                .name(model.getProvider())
                                .build();
                            
                            Mono<Void> modelTask = providerRepository.save(provider)
                                .then(modelRepository.save(model))
                                .doOnSuccess(savedModel -> {
                                    modelCount.incrementAndGet();
                                    log.debug("Model kaydedildi: {}", savedModel.getModelId());
                                })
                                .then();
                            
                            processTasks.add(modelTask);
                        } else {
                            log.warn("Desteklenmeyen model format, atlanıyor: {}", itemMap);
                        }
                    }
                } catch (Exception e) {
                    log.error("Tek bir model işlenirken hata: {}", e.getMessage(), e);
                    // Hatayı yut ve diğer modelleri işlemeye devam et
                }
            }
            
            if (processTasks.isEmpty()) {
                log.warn("İşlenecek model bulunamadı");
                return Mono.just(0);
            }
            
            return Flux.concat(processTasks)
                .then(Mono.just(modelCount.get()));
                
        } catch (Exception e) {
            log.error("JSON model verisi işlenirken hata: {}", e.getMessage(), e);
            return Mono.error(e);
        }
    }
    
    /**
     * Belirtilen JSON dosyasından modelleri yükler.
     * ModelDataLoaderCommand için gereklidir.
     * 
     * @param jsonFilePath JSON dosya yolu
     * @return Yüklenen model sayısı
     */
    public Mono<Integer> loadModelsFromJson(String jsonFilePath) {
        if (jsonFilePath == null || jsonFilePath.isEmpty()) {
            return Mono.error(new IllegalArgumentException("JSON dosya yolu belirtilmemiş"));
        }
        
        log.info("Modeller {} dosyasından yükleniyor", jsonFilePath);
        return loadModels(jsonFilePath);
    }
}
