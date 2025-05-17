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
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Service responsible for loading AI model and provider data from JSON 
 * configuration into the database during application startup.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ModelDataLoader {

    private final ProviderRepository providerRepository;
    private final AIModelRepository aiModelRepository;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;
    
    // Model yükleme işleminin durumunu takip etmek için 
    private final AtomicInteger loadAttempts = new AtomicInteger(0);
    private final ReentrantLock loadLock = new ReentrantLock();
    private final AtomicBoolean loadInProgress = new AtomicBoolean(false);
    private LocalDateTime lastLoadTime;
    private LocalDateTime lastSuccessfulLoadTime;
    private String lastLoadedFile;
    private int lastLoadedModelCount = 0;

    @Value("classpath:${app.models.file:newmodels.json}")
    private Resource modelsResource;
    
    @Value("${app.models.load-on-startup:true}")
    private boolean loadOnStartup;
    
    @Value("${app.models.timeout-seconds:60}")
    private int timeoutSeconds;
    
    @Value("${app.models.retry.max-attempts:3}")
    private int maxRetryAttempts;
    
    @Value("${app.models.retry.delay-seconds:5}")
    private int retryDelaySeconds;
    
    @Value("${app.providers.create-missing:true}")
    private boolean createMissingProviders;

    /**
     * Data transfer object to map the JSON structure to Java objects
     */
    private static class ModelDTO {
        public String modelId;
        public String modelName;
        public String provider;
        public int maxInputTokens;
        public String requiredPlan;
        public int creditCost;
        public String creditType;
        public String category;
        public int contextLength;
    }

    /**
     * Loads model data when the application is ready
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void loadModelData() {
        if (!loadOnStartup) {
            log.info("Model yükleme devre dışı bırakıldı (app.models.load-on-startup=false). Manuel olarak çağrılabilir.");
            return;
        }
        
        // Concurrent yükleme isteklerini önlemek için kilit kullan
        if (!loadLock.tryLock()) {
            log.warn("Model yükleme işlemi zaten devam ediyor. Önceki işlem tamamlanana kadar bekleyin.");
            return;
        }
        
        try {
            if (loadInProgress.getAndSet(true)) {
                log.warn("Başka bir thread model yükleme işlemini başlattı. İşlem zaten devam ediyor.");
                return;
            }
            
            log.info("AI model verilerini yükleme başlıyor - Deneme {}", loadAttempts.incrementAndGet());
            
            // Model yükleme işlemini timeout ile sınırla ve ayrı bir thread'de çalıştır
            loadModelsFromFile(modelsResource.getURL().toString())
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(count -> {
                    lastLoadTime = LocalDateTime.now();
                    lastSuccessfulLoadTime = LocalDateTime.now();
                    lastLoadedModelCount = count;
                    lastLoadedFile = modelsResource.getFilename();
                    log.info("AI model verileri başarıyla yüklendi: {} model", count);
                })
                .doOnError(error -> {
                    lastLoadTime = LocalDateTime.now();
                    log.error("AI model verileri yüklenirken hata oluştu: {}", error.getMessage(), error);
                    
                    if (loadAttempts.get() < maxRetryAttempts) {
                        log.info("{} saniye sonra yeniden deneme yapılacak ({})", 
                                retryDelaySeconds, loadAttempts.get());
                        try {
                            Thread.sleep(retryDelaySeconds * 1000);
                            loadInProgress.set(false);
                            loadLock.unlock();
                            loadModelData(); // Yeniden dene
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    }
                })
                .doFinally(signal -> {
                    loadInProgress.set(false);
                    if (loadLock.isHeldByCurrentThread()) {
                        loadLock.unlock();
                    }
                })
                .subscribe();
                
        } catch (Exception e) {
            loadInProgress.set(false);
            if (loadLock.isHeldByCurrentThread()) {
                loadLock.unlock();
            }
            log.error("Model yükleme işlemi başlatılırken beklenmeyen hata: {}", e.getMessage(), e);
        }
    }

    /**
     * Reads model data from the JSON file
     */
    private List<ModelDTO> readModelsFromJson(String filePath) throws IOException {
        log.debug("JSON dosyasından model verilerini okuma: {}", filePath);
        
        Resource resource = resourceLoader.getResource(filePath.startsWith("classpath:") || 
                                                     filePath.startsWith("file:") || 
                                                     filePath.startsWith("http") ? 
            filePath : "file:" + filePath);
        
        if (!resource.exists()) {
            log.error("Model dosyası bulunamadı: {}", filePath);
            throw new IOException("Model dosyası bulunamadı: " + filePath);
        }
        
        try (InputStream inputStream = resource.getInputStream()) {
            List<ModelDTO> models = objectMapper.readValue(
                inputStream, 
                new TypeReference<List<ModelDTO>>() {}
            );
            log.info("JSON dosyasından {} model okundu", models.size());
            
            // Okunan verileri doğrula
            validateModels(models);
            
            return models;
        } catch (IOException e) {
            log.error("JSON dosyasından model okunamadı: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Validates the models read from JSON
     */
    private void validateModels(List<ModelDTO> models) {
        int invalidModels = 0;
        List<String> invalidModelIds = new ArrayList<>();
        Map<String, Integer> duplicateCheckMap = new HashMap<>();
        
        for (ModelDTO model : models) {
            boolean isValid = true;
            StringBuilder errors = new StringBuilder();
            
            if (model.modelId == null || model.modelId.isEmpty()) {
                errors.append("modelId boş olamaz; ");
                isValid = false;
            } else {
                // Yinelenen modelId'leri kontrol et
                duplicateCheckMap.put(model.modelId, duplicateCheckMap.getOrDefault(model.modelId, 0) + 1);
                if (duplicateCheckMap.get(model.modelId) > 1) {
                    errors.append("Yinelenen modelId: ").append(model.modelId).append("; ");
                    isValid = false;
                }
            }
            
            if (model.modelName == null || model.modelName.isEmpty()) {
                errors.append("modelName boş olamaz; ");
                isValid = false;
            }
            
            if (model.provider == null || model.provider.isEmpty()) {
                errors.append("provider boş olamaz; ");
                isValid = false;
            }
            
            if (model.maxInputTokens <= 0) {
                errors.append("maxInputTokens 0'dan büyük olmalı; ");
                isValid = false;
            }
            
            if (model.contextLength <= 0) {
                errors.append("contextLength 0'dan büyük olmalı; ");
                isValid = false;
            }
            
            if (!isValid) {
                log.warn("Geçersiz model verisi: {} - {}", model.modelId, errors.toString());
                invalidModels++;
                invalidModelIds.add(model.modelId);
            }
        }
        
        if (invalidModels > 0) {
            log.warn("{} model geçersiz veri içeriyor: {}", invalidModels, String.join(", ", invalidModelIds));
            
            // Kritik doğrulama başarısız olursa işlemi durdur
            if (invalidModels > models.size() * 0.2) { // %20'den fazla model geçersizse
                throw new IllegalArgumentException("Model dosyasında çok fazla geçersiz veri bulundu: " + 
                    invalidModels + " / " + models.size());
            }
        }
        
        // Yinelenen modelId'leri kontrol et ve raporla
        List<String> duplicateModelIds = duplicateCheckMap.entrySet().stream()
            .filter(e -> e.getValue() > 1)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
            
        if (!duplicateModelIds.isEmpty()) {
            log.warn("Yinelenen modelId'ler tespit edildi: {}", duplicateModelIds);
        }
    }

    /**
     * Loads model data from a specified file path
     */
    public Mono<Integer> loadModelsFromFile(String filePath) {
        log.info("Dosyadan AI model verilerini yükleme: {}", filePath);
        
        if (loadInProgress.get() && !loadLock.isHeldByCurrentThread()) {
            log.warn("Model yükleme işlemi zaten devam ediyor. İşlem tamamlanana kadar bekleyin.");
            return Mono.error(new IllegalStateException("Model yükleme işlemi zaten devam ediyor"));
        }
        
        return Mono.fromCallable(() -> readModelsFromJson(filePath))
            .flatMap(modelDTOs -> {
                log.info("Dosyadan {} model okundu: {}", modelDTOs.size(), filePath);
                
                // Provider'ları önce işle
                return saveProvidersReactive(modelDTOs)
                    .flatMap(providerMap -> {
                        // Sonra modelleri işle
                        return saveModelsReactive(modelDTOs, providerMap)
                            .doOnSuccess(count -> {
                                log.info("Toplam {} model başarıyla kaydedildi", count);
                                lastLoadedModelCount = count;
                                lastLoadedFile = filePath;
                            });
                    });
            })
            .retryWhen(Retry.backoff(2, Duration.ofSeconds(2))
                .filter(e -> !(e instanceof IllegalArgumentException)) // Doğrulama hatalarını yeniden deneme
                .doBeforeRetry(rs -> log.warn("Model yükleme hatası, yeniden deneniyor: {} ({})", 
                    rs.failure().getMessage(), rs.totalRetries())))
            .onErrorResume(e -> {
                log.error("Model yükleme işlemi başarısız oldu: {}", e.getMessage(), e);
                return Mono.error(e);
            });
    }

    /**
     * Reactive version of saveProviders
     */
    private Mono<Map<String, Provider>> saveProvidersReactive(List<ModelDTO> modelDTOs) {
        // Benzersiz provider isimlerini çıkar ve null/boş olanları filtrele
        Set<String> uniqueProviderNames = modelDTOs.stream()
                .map(dto -> dto.provider)
                .filter(providerName -> providerName != null && !providerName.trim().isEmpty())
                .collect(Collectors.toSet());
        
        log.info("{} benzersiz provider bulundu", uniqueProviderNames.size());
        
        // Eğer toplamdan daha az provider bulunduysa, bazı null/boş olanlar filtrelenmiş demektir
        long originalProviderCount = modelDTOs.stream()
                .map(dto -> dto.provider)
                .distinct()
                .count();
                
        if (originalProviderCount > uniqueProviderNames.size()) {
            log.warn("{} model null veya boş provider içeriyor ve işlenmeyecek", 
                    originalProviderCount - uniqueProviderNames.size());
        }
        
        // Veritabanından mevcut provider'ları getir
        return providerRepository.findAll()
                .collectList()
                .flatMap(existingProviders -> {
                    Map<String, Provider> existingProviderMap = existingProviders.stream()
                            .collect(Collectors.toMap(
                                Provider::getName, 
                                provider -> provider,
                                (p1, p2) -> p1 // Duplicate names durumunda ilkini kullan
                            ));
                    
                    // Henüz var olmayan provider'lar için yenilerini oluştur
                    Map<String, Provider> providerMap = new ConcurrentHashMap<>(existingProviderMap);
                    List<Provider> newProviders = new ArrayList<>();
                    List<Provider> updatedProviders = new ArrayList<>();
                    
                    for (String providerName : uniqueProviderNames) {
                        // Ekstra kontrol: yine de null/boş kontrolü yapalım
                        if (providerName == null || providerName.trim().isEmpty()) {
                            log.warn("Null veya boş provider ismi filtreleme sonrası bulundu, atlanıyor");
                            continue;
                        }
                        
                        if (!providerMap.containsKey(providerName)) {
                            if (!createMissingProviders) {
                                log.warn("Provider bulunamadı ve otomatik oluşturma devre dışı: {}", providerName);
                                continue;
                            }
                            
                            Provider newProvider = Provider.builder()
                                .name(providerName)
                                .active(true)
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build();
                                
                            newProviders.add(newProvider);
                            log.info("Yeni provider hazırlanıyor: {}", newProvider.getName());
                        } else {
                            // Mevcut provider'ı güncelle
                            Provider existingProvider = providerMap.get(providerName);
                            if (!existingProvider.isActive()) {
                                existingProvider.setActive(true);
                                existingProvider.setUpdatedAt(LocalDateTime.now());
                                updatedProviders.add(existingProvider);
                                log.info("Provider yeniden aktifleştiriliyor: {}", providerName);
                            }
                        }
                    }
                    
                    // Önce yeni provider'ları kaydet
                    Mono<List<Provider>> saveNewProvidersMono = newProviders.isEmpty() ? 
                        Mono.just(Collections.emptyList()) : 
                        providerRepository.saveAll(newProviders).collectList();
                    
                    // Sonra güncellenen provider'ları kaydet
                    Mono<List<Provider>> updateProvidersMono = updatedProviders.isEmpty() ? 
                        Mono.just(Collections.emptyList()) : 
                        providerRepository.saveAll(updatedProviders).collectList();
                    
                    // Her iki işlemi de tamamla ve providerMap'i döndür
                    return saveNewProvidersMono.flatMap(savedNewProviders -> {
                        // Yeni provider'ları map'e ekle
                        for (Provider provider : savedNewProviders) {
                            providerMap.put(provider.getName(), provider);
                            log.debug("Yeni provider kaydedildi: {}", provider.getName());
                        }
                        
                        return updateProvidersMono.map(updatedProvidersList -> {
                            // Güncellenen provider'ları map'e ekle (aslında zaten var)
                            for (Provider provider : updatedProvidersList) {
                                log.debug("Provider güncellendi: {}", provider.getName());
                            }
                            
                            log.info("Toplam {} provider işlendi ({} yeni, {} güncellendi)", 
                                savedNewProviders.size() + updatedProvidersList.size(),
                                savedNewProviders.size(), 
                                updatedProvidersList.size());
                                
                            return providerMap;
                        });
                    });
                });
    }

    /**
     * Reactive version of saveModels
     */
    private Mono<Integer> saveModelsReactive(List<ModelDTO> modelDTOs, Map<String, Provider> providerMap) {
        // Sayaç sınıfı ile istatistikleri takip et
        class ModelCounter {
            int updatedCount = 0;
            int newCount = 0;
            int errorCount = 0;
            int skippedCount = 0;
            int invalidProviderCount = 0;
        }
        
        // Veritabanından mevcut modelleri getir
        return aiModelRepository.findAll()
                .collectList()
                .flatMap(existingModels -> {
                    Map<String, AIModel> existingModelMap = existingModels.stream()
                            .collect(Collectors.toMap(
                                AIModel::getModelId, 
                                model -> model,
                                (m1, m2) -> m1 // Duplicate IDs durumunda ilkini kullan
                            ));
                    
                    List<AIModel> modelsToSave = new ArrayList<>();
                    ModelCounter counter = new ModelCounter();
                    List<String> processedModelIds = new ArrayList<>();
                    
                    for (ModelDTO dto : modelDTOs) {
                        try {
                            // ModelId kontrolü
                            if (dto.modelId == null || dto.modelId.trim().isEmpty()) {
                                log.warn("ModelId null veya boş, model atlanıyor");
                                counter.skippedCount++;
                                continue;
                            }
                            
                            // Eğer bu modelId zaten işlendiyse, atla (yinelenen modelId'ler için)
                            if (processedModelIds.contains(dto.modelId)) {
                                log.warn("Yinelenen modelId atlanıyor: {}", dto.modelId);
                                counter.skippedCount++;
                                continue;
                            }
                            
                            processedModelIds.add(dto.modelId);
                            
                            // Provider kontrolü
                            if (dto.provider == null || dto.provider.trim().isEmpty()) {
                                log.warn("Model için provider null veya boş: {}", dto.modelId);
                                counter.invalidProviderCount++;
                                continue;
                            }
                            
                            Provider provider = providerMap.get(dto.provider);
                            
                            if (provider == null) {
                                log.warn("Model için provider bulunamadı {}: {}", dto.modelId, dto.provider);
                                counter.errorCount++;
                                continue;
                            }
                            
                            AIModel model;
                            boolean isNewModel = false;
                            
                            // Modelin halihazırda var olup olmadığını kontrol et
                            if (existingModelMap.containsKey(dto.modelId)) {
                                // Mevcut modeli güncelle
                                model = existingModelMap.get(dto.modelId);
                                counter.updatedCount++;
                            } else {
                                // Yeni model oluştur
                                model = new AIModel();
                                model.setModelId(dto.modelId);
                                model.setCreatedAt(LocalDateTime.now());
                                isNewModel = true;
                                counter.newCount++;
                            }
                            
                            // Model alanlarını güncelle
                            model.setModelName(dto.modelName);
                            model.setProviderId(provider.getId());
                            model.setProvider(provider.getName()); 
                            model.setMaxInputTokens(dto.maxInputTokens);
                            model.setRequiredPlan(dto.requiredPlan);
                            model.setCreditCost(dto.creditCost);
                            model.setCreditType(dto.creditType);
                            model.setCategory(dto.category);
                            model.setContextLength(dto.contextLength);
                            model.setActive(true);
                            model.setUpdatedAt(LocalDateTime.now());
                            
                            modelsToSave.add(model);
                            
                            if (isNewModel) {
                                log.debug("Yeni model hazırlandı: {}", dto.modelId);
                            } else {
                                log.debug("Mevcut model güncellendi: {}", dto.modelId);
                            }
                        } catch (Exception e) {
                            log.error("Model işlenirken hata oluştu {}: {}", dto.modelId, e.getMessage(), e);
                            counter.errorCount++;
                        }
                    }
                    
                    if (!modelsToSave.isEmpty()) {
                        log.info("{} model kaydediliyor ({} yeni, {} güncelleme, {} hatalı, {} atlandı, {} geçersiz provider)", 
                            modelsToSave.size(), counter.newCount, counter.updatedCount, 
                            counter.errorCount, counter.skippedCount, counter.invalidProviderCount);
                        
                        // Modelleri toplu olarak kaydet, daha verimli
                        return aiModelRepository.saveAll(modelsToSave)
                                .collectList()
                                .map(savedModels -> {
                                    log.info("{} model başarıyla kaydedildi", savedModels.size());
                                    if (counter.errorCount > 0) {
                                        log.warn("{} modelde hata oluştu ve kaydedilemedi", counter.errorCount);
                                    }
                                    return modelsToSave.size();
                                });
                    } else {
                        log.info("Kaydedilecek model yok");
                        return Mono.just(0);
                    }
                });
    }
    
    /**
     * Manual trigger for reloading model data
     */
    public Mono<Integer> reloadModelData() {
        log.info("Model verilerinin manuel olarak yeniden yüklenmesi başlatılıyor");
        loadAttempts.set(0); // Sayaçları sıfırla
        
        // Kilit kullanarak concurrent yükleme isteklerini önle
        if (!loadLock.tryLock()) {
            log.warn("Model yükleme işlemi zaten devam ediyor. İşlem tamamlanana kadar bekleyin.");
            return Mono.error(new IllegalStateException("Model yükleme işlemi zaten devam ediyor"));
        }
        
        try {
            if (loadInProgress.getAndSet(true)) {
                loadLock.unlock();
                log.warn("Başka bir thread model yükleme işlemini başlattı. İşlem zaten devam ediyor.");
                return Mono.error(new IllegalStateException("Model yükleme işlemi zaten devam ediyor"));
            }
            
            return loadModelsFromFile(modelsResource.getURL().toString())
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .doOnSuccess(count -> {
                    lastLoadTime = LocalDateTime.now();
                    lastSuccessfulLoadTime = LocalDateTime.now();
                    log.info("AI model verileri başarıyla yüklendi: {} model", count);
                })
                .doOnError(error -> {
                    lastLoadTime = LocalDateTime.now();
                    log.error("AI model verileri yüklenirken hata oluştu: {}", error.getMessage());
                })
                .doFinally(signal -> {
                    loadInProgress.set(false);
                    loadLock.unlock();
                });
        } catch (Exception e) {
            loadInProgress.set(false);
            loadLock.unlock();
            log.error("Model yükleme işlemi başlatılırken beklenmeyen hata: {}", e.getMessage(), e);
            return Mono.error(e);
        }
    }
    
    /**
     * Get current model loading status
     */
    public Map<String, Object> getModelLoadingStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("loadInProgress", loadInProgress.get());
        status.put("loadAttempts", loadAttempts.get());
        status.put("lastLoadTime", lastLoadTime);
        status.put("lastSuccessfulLoadTime", lastSuccessfulLoadTime);
        status.put("configuredModelFile", modelsResource.getFilename());
        status.put("lastLoadedFile", lastLoadedFile);
        status.put("lastLoadedModelCount", lastLoadedModelCount);
        return status;
    }
}
