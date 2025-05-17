package com.craftpilot.userservice.service;

import com.craftpilot.userservice.model.ai.AIModel;
import com.craftpilot.userservice.model.ai.Provider;
import com.craftpilot.userservice.repository.AIModelRepository;
import com.craftpilot.userservice.repository.ProviderRepository;
import com.craftpilot.userservice.util.ModelDataFixer;
import com.mongodb.MongoWriteException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class ModelDataLoader {

    private final AIModelRepository modelRepository;
    private final ProviderRepository providerRepository;
    private final ModelDataFixer modelDataFixer;
    private final ResourceLoader resourceLoader;

    /**
     * Uygulama başlangıcında varsayılan modelleri yükler
     */
    @PostConstruct
    public void loadDefaultModels() {
        log.info("Varsayılan modelleri yükleme girişimi başlatılıyor");
        
        // Önce model sayısını kontrol et, eğer veri zaten varsa tekrar yükleme
        modelRepository.count()
            .flatMap(modelCount -> {
                if (modelCount > 0) {
                    log.info("Veritabanında zaten {} model var, yeniden yükleme yapılmayacak", modelCount);
                    return Mono.just(0);
                }

                log.info("Veritabanında hiç model yok, varsayılan modelleri yüklüyorum");
                return loadModelsFromJson("newmodels.json")
                       .onErrorResume(e -> {
                           log.error("Varsayılan model yükleme hatası: {}", e.getMessage(), e);
                           // Alternatif dosya adı dene
                           return loadModelsFromJson("classpath:newmodels.json");
                       });
            })
            .subscribe(
                count -> log.info("Varsayılan model yükleme işlemi tamamlandı: {} model yüklendi", count),
                error -> log.error("Varsayılan model yükleme hatası: {}", error.getMessage(), error)
            );
    }

    /**
     * JSON dosyasından model verilerini yükler
     *
     * @param jsonFilePath JSON dosya yolu
     * @return Yüklenen model sayısı
     */
    public Mono<Integer> loadModelsFromJson(String jsonFilePath) {
        log.info("Modeller {} yolundan yükleniyor", jsonFilePath);

        return readJsonContent(jsonFilePath)
                .flatMap(jsonContent -> {
                    Map<String, Object> validatedData = modelDataFixer.validateAndFixModelData(jsonContent);
                    List<AIModel> models = (List<AIModel>) validatedData.get("models");
                    List<Provider> providers = (List<Provider>) validatedData.get("providers");

                    log.info("Doğrulanmış veri: {} model ve {} sağlayıcı", 
                            models.size(), providers.size());

                    return saveAllProvidersThenModels(providers, models);
                })
                .onErrorResume(e -> {
                    log.error("Model yükleme hatası: {}", e.getMessage(), e);
                    return Mono.just(0);
                });
    }

    /**
     * Önce tüm sağlayıcıları sonra tüm modelleri kaydeder
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
                .concatMap(model -> saveModel(model, savedModelsCount))
                .then(Mono.just(savedModelsCount.get()))
                .doOnSuccess(count -> log.info("{} adet model başarıyla yüklendi", count));
    }

    /**
     * Provider'ı kaydeder, zaten varsa var olanı döndürür
     */
    private Mono<Provider> saveProvider(Provider provider) {
        if (provider.getName() == null || provider.getName().trim().isEmpty()) {
            log.warn("Geçersiz provider ismi, atlanan provider: {}", provider);
            return Mono.empty();
        }
        
        return providerRepository.findByName(provider.getName())
                .flatMap(existingProvider -> {
                    log.info("Provider '{}' zaten var, yeniden kullanılıyor", provider.getName());
                    return Mono.just(existingProvider);
                })
                .switchIfEmpty(
                        providerRepository.save(provider)
                                .doOnSuccess(savedProvider -> 
                                    log.info("Yeni provider kaydedildi: {}", savedProvider.getName()))
                                .doOnError(e -> 
                                    log.error("Provider '{}' kaydedilirken hata: {}", 
                                        provider.getName(), e.getMessage()))
                );
    }

    /**
     * Modeli kaydeder, hata durumunda loglar ve devam eder
     */
    private Mono<AIModel> saveModel(AIModel model, AtomicInteger counter) {
        // Eksik veya geçersiz modelId varsa atla
        if (model.getModelId() == null || model.getModelId().isEmpty()) {
            log.warn("Geçersiz modelId, atlanan model: {}", model.getModelName());
            return Mono.empty();
        }

        return modelRepository.findByModelId(model.getModelId())
                .flatMap(existingModel -> {
                    log.debug("Model zaten var, güncelleniyor: {}", model.getModelId());
                    // Model ID'yi koru, diğer alanları güncelle
                    model.setId(existingModel.getId());
                    return modelRepository.save(model);
                })
                .switchIfEmpty(
                        modelRepository.save(model)
                                .doOnSuccess(savedModel -> {
                                    counter.incrementAndGet();
                                    log.debug("Yeni model kaydedildi: {}", savedModel.getModelId());
                                })
                )
                .onErrorResume(e -> {
                    if (e instanceof DuplicateKeyException || e instanceof MongoWriteException) {
                        log.error("Model '{}' kaydedilirken hata: {}", 
                                model.getModelId(), e.getMessage());
                    } else {
                        log.error("Beklenmeyen hata: {}", e.getMessage(), e);
                    }
                    return Mono.empty();
                });
    }

    /**
     * JSON içeriğini dosya yolundan okur
     */
    private Mono<String> readJsonContent(String jsonFilePath) {
        try {
            Resource resource;
            
            if (jsonFilePath.startsWith("classpath:")) {
                // ClassPath kaynaklarını yükle
                resource = resourceLoader.getResource(jsonFilePath);
            } else if (Files.exists(Paths.get(jsonFilePath))) {
                // Dosya sisteminden yükle
                resource = new FileSystemResource(jsonFilePath);
            } else {
                // Önce sınıf yolunda ara
                resource = new ClassPathResource(jsonFilePath);
                if (!resource.exists()) {
                    // Sonra resources klasöründe ara
                    resource = resourceLoader.getResource("classpath:" + jsonFilePath);
                    if (!resource.exists()) {
                        throw new IOException("Dosya bulunamadı: " + jsonFilePath);
                    }
                }
            }
            
            log.info("Model dosyası bulundu: {}", resource.getURL());
            
            try (var inputStream = resource.getInputStream()) {
                byte[] bytes = inputStream.readAllBytes();
                String content = new String(bytes, StandardCharsets.UTF_8);
                
                if (content.isEmpty()) {
                    throw new IOException("JSON dosyası boş");
                }
                
                return Mono.just(content);
            }
        } catch (IOException e) {
            log.error("JSON dosyası okunamadı: {}", e.getMessage(), e);
            return Mono.error(e);
        }
    }
}
