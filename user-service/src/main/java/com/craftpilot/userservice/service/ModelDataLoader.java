package com.craftpilot.userservice.service;

import com.craftpilot.userservice.model.ai.AIModel;
import com.craftpilot.userservice.model.ai.Provider;
import com.craftpilot.userservice.repository.AIModelRepository;
import com.craftpilot.userservice.repository.ProviderRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import static java.util.Map.entry;

/**
 * AI Modellerini ve Provider'ları JSON dosyasından yükleyip veritabanına kaydeden servis
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ModelDataLoader {

    private final AIModelRepository modelRepository;
    private final ProviderRepository providerRepository;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    // Icon isimlerini provider'lara eşleştiren map
    private static final Map<String, String> PROVIDER_ICONS = Map.ofEntries(
            entry("openai", "TbBrandOpenai"),
            entry("google", "TbBrandGoogle"),
            entry("anthropic", "SiAnthropic"),
            entry("meta", "TbBrandMeta"),
            entry("mistral", "SiMistral"),
            entry("cohere", "SiCohere"),
            entry("nvidia", "TbBrandNvidia"),
            entry("microsoft", "TbMicrosoft"),
            entry("perplexity", "TbBrain"),
            entry("qwen", "SiQiita"),
            entry("deepseek", "TbSearch"),
            entry("liquid", "TbDroplet"),
            entry("ai21", "TbNumber21"),
            entry("x-ai", "TbBrandX")
    );

    // Varsayılan icon
    private static final String DEFAULT_ICON = "TbBrain";

    @Value("${app.load-models-on-startup:true}")
    private boolean loadModelsOnStartup;

    @Value("${app.models.default-file:newmodels.json}")
    private String defaultModelFile;
    
    @Value("${app.models.additional-files:}")
    private String additionalModelFiles;

    /**
     * Uygulama başlangıcında modelleri yükler
     */
    @PostConstruct
    public void loadDefaultModels() {
        if (!loadModelsOnStartup) {
            log.info("Model otomatik yükleme devre dışı (app.load-models-on-startup=false)");
            return;
        }
        
        log.info("Varsayılan AI modelleri yükleme işlemi başlatılıyor");
        
        // Önce model sayısını kontrol et, eğer veri zaten varsa tekrar yükleme
        modelRepository.count()
            .flatMap(modelCount -> {
                if (modelCount > 0) {
                    log.info("Veritabanında zaten {} model var, varsayılan modeller yüklenmeyecek", modelCount);
                    return Mono.just(0);
                }

                log.info("Veritabanında hiç model yok, varsayılan modeller yükleniyor");
                return loadModelsFromFile(defaultModelFile)
                    .onErrorResume(e -> {
                        log.error("Varsayılan model dosyası yüklenirken hata: {}", e.getMessage());
                        // ClassPath'de farklı konumlarda dene
                        return loadModelsFromFile("classpath:" + defaultModelFile)
                            .onErrorResume(e2 -> loadModelsFromFile("classpath:newmodels.json"));
                    });
            })
            .subscribe(
                count -> log.info("Varsayılan model yükleme işlemi tamamlandı: {} model yüklendi", count),
                error -> log.error("Model yükleme işleminde hata: {}", error.getMessage(), error)
            );
            
        // Ek model dosyalarını yükle (eğer belirtilmişse)
        if (additionalModelFiles != null && !additionalModelFiles.trim().isEmpty()) {
            String[] fileNames = additionalModelFiles.split(",");
            for (String fileName : fileNames) {
                if (fileName != null && !fileName.trim().isEmpty()) {
                    loadModelsFromFile(fileName.trim())
                        .subscribe(
                            count -> log.info("Ek model dosyası ({}) yükleme tamamlandı: {} model yüklendi", 
                                    fileName.trim(), count),
                            error -> log.error("Ek model dosyası ({}) yüklenirken hata: {}", 
                                    fileName.trim(), error.getMessage())
                        );
                }
            }
        }
    }
    
    /**
     * Belirtilen dosyadan modelleri yükler
     * 
     * @param filePath Model dosyasının yolu
     * @return Yüklenen model sayısı
     */
    public Mono<Integer> loadModelsFromFile(String filePath) {
        log.info("'{}' dosyasından AI modelleri yükleniyor", filePath);
        
        return readJsonContent(filePath)
            .flatMap(jsonContent -> {
                try {
                    // JSON içeriğini model listesi olarak parse et
                    List<AIModel> models = parseModelList(jsonContent);
                    
                    if (models.isEmpty()) {
                        log.warn("JSON dosyasında model bulunamadı: {}", filePath);
                        return Mono.just(0);
                    }
                    
                    log.info("{} dosyasında {} model bulundu", filePath, models.size());
                    
                    // Modelleri doğrula ve geçersiz olanları filtrele
                    List<AIModel> validModels = models.stream()
                        .filter(this::validateModel)
                        .toList();
                    
                    if (validModels.size() < models.size()) {
                        log.warn("{} model geçersiz veri nedeniyle atlandı", models.size() - validModels.size());
                    }
                    
                    // Provider'ları modeller üzerinden çıkar
                    List<Provider> providers = extractProvidersFromModels(validModels);
                    log.info("{} farklı provider bulundu", providers.size());
                    
                    // Önce provider'ları, sonra modelleri kaydet
                    return saveAllProvidersThenModels(providers, validModels);
                    
                } catch (Exception e) {
                    log.error("JSON işlenirken hata: {}", e.getMessage(), e);
                    return Mono.error(e);
                }
            })
            .onErrorResume(e -> {
                log.error("Model yükleme işlemi başarısız: {}", e.getMessage(), e);
                return Mono.just(0); // Hata durumunda 0 döndür
            });
    }
    
    /**
     * JSON içeriğini model listesine dönüştürür
     * 
     * @param jsonContent JSON içeriği
     * @return Model listesi
     */
    private List<AIModel> parseModelList(String jsonContent) throws JsonProcessingException {
        try {
            // Önce bir dizi olarak okumayı dene
            return objectMapper.readValue(jsonContent, new TypeReference<List<AIModel>>() {});
        } catch (JsonProcessingException e) {
            log.warn("JSON doğrudan dizi olarak okunamadı, 'models' anahtarını deniyorum");
            
            // Eski format olabilir, "models" anahtarını dene
            try {
                Map<String, Object> jsonMap = objectMapper.readValue(jsonContent, new TypeReference<Map<String, Object>>() {});
                if (jsonMap.containsKey("models")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> modelMaps = (List<Map<String, Object>>) jsonMap.get("models");
                    
                    List<AIModel> models = new ArrayList<>();
                    for (Map<String, Object> modelMap : modelMaps) {
                        models.add(objectMapper.convertValue(modelMap, AIModel.class));
                    }
                    return models;
                }
            } catch (Exception ignored) {
                // İkinci deneme de başarısız olursa, asıl hatayı yeniden fırlat
            }
            
            // Hiçbir format uyuşmadıysa, orijinal hatayı fırlat
            throw e;
        }
    }

    /**
     * Modelin geçerli olup olmadığını kontrol eder
     * 
     * @param model Kontrol edilecek model
     * @return Model geçerli ise true, değilse false
     */
    private boolean validateModel(AIModel model) {
        if (model.getModelId() == null || model.getModelId().trim().isEmpty()) {
            log.warn("Geçersiz model: modelId boş");
            return false;
        }
        
        if (model.getProvider() == null || model.getProvider().trim().isEmpty()) {
            log.warn("Geçersiz model: provider boş, model={}", model.getModelId());
            return false;
        }
        
        // Varsayılan değerleri ata, eğer eksikse
        if (model.getMaxInputTokens() == null) {
            model.setMaxInputTokens(8000);
        }
        
        if (model.getRequiredPlan() == null) {
            model.setRequiredPlan("FREE");
        }
        
        if (model.getCreditCost() == null) {
            model.setCreditCost(1);
        }
        
        if (model.getCreditType() == null) {
            model.setCreditType("STANDARD");
        }
        
        if (model.getCategory() == null) {
            model.setCategory(model.getRequiredPlan());
        }
        
        if (model.getIsActive() == null) {
            model.setIsActive(true);
        }
        
        return true;
    }
    
    /**
     * Model listesinden unique provider'ları çıkarır
     * 
     * @param models Model listesi
     * @return Provider listesi
     */
    private List<Provider> extractProvidersFromModels(List<AIModel> models) {
        Map<String, Provider> providerMap = new ConcurrentHashMap<>();
        
        for (AIModel model : models) {
            String providerName = model.getProvider();
            if (!providerMap.containsKey(providerName)) {
                Provider provider = Provider.builder()
                        .name(providerName)
                        .icon(getProviderIcon(providerName))
                        .description(generateProviderDescription(providerName))
                        .build();
                providerMap.put(providerName, provider);
            }
        }
        
        return new ArrayList<>(providerMap.values());
    }
    
    /**
     * Provider adına göre icon bulur
     * 
     * @param providerName Provider adı
     * @return Icon adı
     */
    private String getProviderIcon(String providerName) {
        if (providerName == null) return DEFAULT_ICON;
        
        String normalizedName = providerName.toLowerCase();
        
        // Önce tam eşleşme kontrolü
        if (PROVIDER_ICONS.containsKey(normalizedName)) {
            return PROVIDER_ICONS.get(normalizedName);
        }
        
        // Kısmi eşleşme kontrolü
        for (Map.Entry<String, String> entry : PROVIDER_ICONS.entrySet()) {
            if (normalizedName.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        
        return DEFAULT_ICON;
    }
    
    /**
     * Provider için açıklama oluşturur
     * 
     * @param providerName Provider adı
     * @return Açıklama metni
     */
    private String generateProviderDescription(String providerName) {
        if (providerName == null) return "AI Modelleri";
        return providerName + " AI modelleri";
    }

    /**
     * Önce tüm provider'ları sonra tüm modelleri kaydeder
     * 
     * @param providers Kaydedilecek provider listesi
     * @param models Kaydedilecek model listesi
     * @return Kaydedilen model sayısı
     */
    private Mono<Integer> saveAllProvidersThenModels(List<Provider> providers, List<AIModel> models) {
        AtomicInteger savedModelsCount = new AtomicInteger(0);
        
        // Önce provider'ları kaydet, sonra modelleri kaydet
        return Flux.fromIterable(providers)
                .flatMap(this::saveProvider)
                .collectList()
                .doOnSuccess(savedProviders -> 
                    log.info("{} provider başarıyla kaydedildi", savedProviders.size()))
                .flatMapMany(savedProviders -> Flux.fromIterable(models))
                .flatMap(model -> saveModel(model, savedModelsCount))
                .collectList()
                .map(saved -> savedModelsCount.get())
                .onErrorResume(e -> {
                    log.error("Toplu kayıt işleminde hata: {}", e.getMessage(), e);
                    return Mono.just(savedModelsCount.get());
                });
    }

    /**
     * Provider'ı kaydeder, zaten varsa var olan kayıtı günceller
     * 
     * @param provider Kaydedilecek provider
     * @return Kaydedilen veya güncellenen provider
     */
    private Mono<Provider> saveProvider(Provider provider) {
        // Provider adı geçersizse atla
        if (provider.getName() == null || provider.getName().trim().isEmpty()) {
            log.warn("Geçersiz provider adı, provider atlanıyor");
            return Mono.empty();
        }
        
        return providerRepository.findByName(provider.getName())
                .flatMap(existingProvider -> {
                    log.debug("Provider '{}' zaten var, eksik bilgiler güncelleniyor", provider.getName());
                    
                    boolean updated = false;
                    
                    // Eksik bilgileri güncelle
                    if (existingProvider.getIcon() == null && provider.getIcon() != null) {
                        existingProvider.setIcon(provider.getIcon());
                        updated = true;
                    }
                    
                    if (existingProvider.getDescription() == null && provider.getDescription() != null) {
                        existingProvider.setDescription(provider.getDescription());
                        updated = true;
                    }
                    
                    if (updated) {
                        return providerRepository.save(existingProvider)
                            .doOnSuccess(p -> log.debug("Provider güncellendi: {}", p.getName()));
                    }
                    
                    return Mono.just(existingProvider);
                })
                .switchIfEmpty(
                    providerRepository.save(provider)
                        .doOnSuccess(savedProvider -> 
                            log.info("Yeni provider kaydedildi: {}", savedProvider.getName()))
                        .onErrorResume(e -> {
                            if (e instanceof DuplicateKeyException) {
                                log.warn("Provider kaydedilirken çakışma: {} - tekrar deneniyor", provider.getName());
                                return providerRepository.findByName(provider.getName())
                                        .switchIfEmpty(Mono.defer(() -> {
                                            log.warn("Provider bulunamadı, yeniden kayıt deneniyor");
                                            return providerRepository.save(Provider.builder()
                                                    .name(provider.getName())
                                                    .icon(provider.getIcon())
                                                    .description(provider.getDescription())
                                                    .build());
                                        }));
                            }
                            log.error("Provider kaydedilirken beklenmeyen hata: {}", e.getMessage(), e);
                            return Mono.empty();
                        })
                );
    }

    /**
     * Modeli kaydeder ve kaydedilen model sayısını günceller
     * 
     * @param model Kaydedilecek model
     * @param counter Kaydedilen model sayacı
     * @return Kaydedilen model
     */
    private Mono<AIModel> saveModel(AIModel model, AtomicInteger counter) {
        // Model ID boş veya null ise atla
        if (model.getModelId() == null || model.getModelId().isEmpty()) {
            log.warn("Geçersiz model ID, model atlanıyor");
            return Mono.empty();
        }

        // Provider bilgisi boş veya null ise uyarı ver ve atla
        if (model.getProvider() == null || model.getProvider().isEmpty()) {
            log.warn("Provider bilgisi eksik, model atlanıyor: {}", model.getModelId());
            return Mono.empty();
        }

        // Önce model ID'ye göre kontrol et
        return modelRepository.findByModelId(model.getModelId())
                .flatMap(existingModel -> {
                    log.debug("Model ID '{}' zaten var, güncelleniyor", model.getModelId());
                    // ID ve oluşturma bilgilerini koru, diğer alanları güncelle 
                    model.setId(existingModel.getId());
                    return modelRepository.save(model)
                            .doOnSuccess(updatedModel -> 
                                log.debug("Model güncellendi: {}", updatedModel.getModelId()));
                })
                .switchIfEmpty(
                        modelRepository.save(model)
                                .doOnSuccess(savedModel -> {
                                    counter.incrementAndGet();
                                    log.info("Yeni model kaydedildi: {} - {}", 
                                            savedModel.getModelId(), savedModel.getModelName());
                                })
                )
                .onErrorResume(e -> {
                    if (e instanceof DuplicateKeyException) {
                        log.warn("Model kaydedilirken çakışma: {} - atlanıyor", model.getModelId());
                    } else {
                        log.error("Model '{}' kaydedilirken hata: {}", 
                                model.getModelId(), e.getMessage(), e);
                    }
                    return Mono.empty();
                });
    }

    /**
     * JSON içeriğini dosya yolundan veya classpath'den okur
     * 
     * @param jsonFilePath JSON dosyasının yolu
     * @return JSON içeriği
     */
    private Mono<String> readJsonContent(String jsonFilePath) {
        try {
            Resource resource;
            
            // Kaynak türüne göre Resource nesnesi oluştur
            if (jsonFilePath.startsWith("classpath:")) {
                // ClassPath kaynaklarını yükle
                String path = jsonFilePath.substring("classpath:".length());
                resource = resourceLoader.getResource("classpath:" + path);
                log.debug("ClassPath kaynağı yükleniyor: {}", path);
            } else if (Files.exists(Paths.get(jsonFilePath))) {
                // Dosya sisteminden yükle
                resource = resourceLoader.getResource("file:" + jsonFilePath);
                log.debug("Dosya sisteminden kaynak yükleniyor: {}", jsonFilePath);
            } else {
                // Önce sınıf yolunda ara
                resource = new ClassPathResource(jsonFilePath);
                if (!resource.exists()) {
                    // Sonra genel resources klasöründe ara
                    resource = resourceLoader.getResource("classpath:" + jsonFilePath);
                    if (!resource.exists()) {
                        return Mono.error(new IOException("Dosya bulunamadı: " + jsonFilePath));
                    }
                }
            }
            
            log.info("Model dosyası bulundu: {}", resource.getURI());
            
            try (InputStream inputStream = resource.getInputStream()) {
                byte[] bytes = inputStream.readAllBytes();
                String content = new String(bytes, StandardCharsets.UTF_8);
                
                if (content.isEmpty()) {
                    return Mono.error(new IOException("JSON dosyası boş: " + jsonFilePath));
                }
                
                log.debug("JSON içeriği başarıyla okundu ({} karakter)", content.length());
                return Mono.just(content);
            }
        } catch (IOException e) {
            log.error("JSON dosyası okunamadı: {}", e.getMessage(), e);
            return Mono.error(e);
        }
    }
}
