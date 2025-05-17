package com.craftpilot.userservice.service;

import com.craftpilot.userservice.model.ai.AIModel;
import com.craftpilot.userservice.model.ai.Provider;
import com.craftpilot.userservice.repository.AIModelRepository;
import com.craftpilot.userservice.repository.ProviderRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
@RequiredArgsConstructor
public class ModelDataLoader {

    private final AIModelRepository modelRepository;
    private final ProviderRepository providerRepository;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    
    private final AtomicBoolean loadInProgress = new AtomicBoolean(false);
    private Integer lastLoadedCount = 0;
    private String lastError = null;
    
    public Map<String, Object> getModelLoadingStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("loadInProgress", loadInProgress.get());
        status.put("lastLoadedCount", lastLoadedCount);
        status.put("lastError", lastError);
        return status;
    }

    /**
     * Load AI models and providers from a JSON file.
     * @param jsonFilePath Path to the JSON file containing models and providers
     * @return A Mono emitting the number of models loaded
     */
    public Mono<Integer> loadModelsFromFile(String jsonFilePath) {
        if (!loadInProgress.compareAndSet(false, true)) {
            log.warn("Model yükleme işlemi zaten devam ediyor, yeni istek göz ardı edildi");
            return Mono.just(lastLoadedCount);
        }
        
        lastError = null;
        
        log.info("AI modelleri dosyadan yüklemeye başlanıyor: {}", jsonFilePath);
        
        return Mono.fromCallable(() -> {
            try {
                Resource resource = resourceLoader.getResource(jsonFilePath);
                if (!resource.exists()) {
                    throw new IOException("Kaynak bulunamadı: " + jsonFilePath);
                }
                return resource.getInputStream();
            } catch (IOException e) {
                log.error("Kaynak yüklenirken hata: {}", jsonFilePath, e);
                throw new RuntimeException("Kaynak yüklenirken hata: " + jsonFilePath, e);
            }
        })
        .flatMap(this::parseJsonData)
        .flatMap(data -> {
            List<AIModel> models = data.getFirst();
            List<Provider> providers = data.getSecond();
            
            log.info("JSON dosyasından {} model ve {} sağlayıcı ayrıştırıldı", 
                     models.size(), providers.size());
            
            // Provider ve modelleri paralel olarak kaydet
            return Mono.zip(
                saveProviders(providers),
                saveModels(models)
            ).map(tuple -> tuple.getT2()); // Yüklenen model sayısını döndür
        })
        .doOnSuccess(count -> {
            lastLoadedCount = count;
            log.info("Başarıyla {} adet AI model yüklendi", count);
        })
        .doOnError(e -> {
            lastError = e.getMessage();
            log.error("AI modelleri yüklenirken hata: {}", e.getMessage(), e);
        })
        .doFinally(signalType -> loadInProgress.set(false));
    }

    private Mono<Pair<List<AIModel>, List<Provider>>> parseJsonData(InputStream inputStream) {
        return Mono.fromCallable(() -> {
            try {
                JsonNode rootNode = objectMapper.readTree(inputStream);
                
                List<AIModel> models = new ArrayList<>();
                List<Provider> providers = new ArrayList<>();
                
                JsonNode modelsNode = rootNode.get("models");
                if (modelsNode != null && modelsNode.isArray()) {
                    modelsNode.forEach(modelNode -> {
                        try {
                            AIModel model = objectMapper.treeToValue(modelNode, AIModel.class);
                            if (model.getModelId() != null && !model.getModelId().trim().isEmpty()) {
                                models.add(model);
                            } else {
                                log.warn("Geçersiz model (modelId boş) atlandı");
                            }
                        } catch (Exception e) {
                            log.warn("Geçersiz model ayrıştırılırken hata, bu öğe atlanıyor: {}", e.getMessage());
                        }
                    });
                    log.info("JSON'dan {} model ayrıştırıldı", models.size());
                } else {
                    log.warn("JSON'da 'models' dizisi bulunamadı");
                }
                
                JsonNode providersNode = rootNode.get("providers");
                if (providersNode != null && providersNode.isArray()) {
                    providersNode.forEach(providerNode -> {
                        try {
                            Provider provider = objectMapper.treeToValue(providerNode, Provider.class);
                            if (provider.getName() != null && !provider.getName().trim().isEmpty()) {
                                providers.add(provider);
                            } else {
                                log.warn("Geçersiz sağlayıcı (name boş) atlandı");
                            }
                        } catch (Exception e) {
                            log.warn("Geçersiz sağlayıcı ayrıştırılırken hata, bu öğe atlanıyor: {}", e.getMessage());
                        }
                    });
                    log.info("JSON'dan {} sağlayıcı ayrıştırıldı", providers.size());
                } else {
                    log.warn("JSON'da 'providers' dizisi bulunamadı");
                }
                
                return new Pair<>(models, providers);
            } catch (IOException e) {
                log.error("JSON verisi ayrıştırılırken hata", e);
                throw new RuntimeException("JSON verisi ayrıştırılırken hata", e);
            }
        });
    }

    private Mono<Integer> saveModels(List<AIModel> models) {
        if (models.isEmpty()) {
            log.warn("Kaydedilecek model yok");
            return Mono.just(0);
        }
        
        log.info("Veritabanına {} model kaydediliyor", models.size());
        
        return Flux.fromIterable(models)
            .flatMap(model -> {
                // Zaman damgalarını ayarla
                LocalDateTime now = LocalDateTime.now();
                if (model.getCreatedAt() == null) {
                    model.setCreatedAt(now);
                }
                model.setUpdatedAt(now);
                
                return modelRepository.findByModelId(model.getModelId())
                    .flatMap(existingModel -> {
                        // Mevcut kaydı güncellemek için ID'yi ayarla
                        model.setId(existingModel.getId());
                        log.debug("Mevcut model güncelleniyor: {}", model.getModelId());
                        return modelRepository.save(model);
                    })
                    .switchIfEmpty(Mono.defer(() -> {
                        log.debug("Yeni model ekleniyor: {}", model.getModelId());
                        return modelRepository.save(model);
                    }));
            })
            .count()
            .map(Long::intValue)
            .doOnSuccess(count -> log.info("Başarıyla {} model kaydedildi", count));
    }

    private Mono<Integer> saveProviders(List<Provider> providers) {
        if (providers.isEmpty()) {
            log.warn("Kaydedilecek sağlayıcı yok");
            return Mono.just(0);
        }
        
        log.info("Veritabanına {} sağlayıcı kaydediliyor", providers.size());
        
        return Flux.fromIterable(providers)
            .flatMap(provider -> {
                // Zaman damgalarını ayarla
                LocalDateTime now = LocalDateTime.now();
                if (provider.getCreatedAt() == null) {
                    provider.setCreatedAt(now);
                }
                provider.setUpdatedAt(now);
                
                return providerRepository.save(provider)
                    .doOnSuccess(saved -> log.debug("Sağlayıcı kaydedildi: {}", saved.getName()));
            })
            .count()
            .map(Long::intValue)
            .doOnSuccess(count -> log.info("Başarıyla {} sağlayıcı kaydedildi", count));
    }
    
    // İki değer döndürmek için basit Pair sınıfı
    private static class Pair<T, U> {
        private final T first;
        private final U second;
        
        public Pair(T first, U second) {
            this.first = first;
            this.second = second;
        }
        
        public T getFirst() {
            return first;
        }
        
        public U getSecond() {
            return second;
        }
    }
}
