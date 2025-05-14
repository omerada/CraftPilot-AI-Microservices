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
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ModelDataLoader {

    private final ProviderRepository providerRepository;
    private final AIModelRepository modelRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.data.models-path:classpath:data/models.json}")
    private String modelsPath;

    /**
     * Yapılandırılabilir JSON dosya yolundan modelleri yükler ve MongoDB'ye
     * kaydeder
     * 
     * @param configuredPath Yapılandırmadan gelen dosya yolu (null ise varsayılan
     *                       kullanılır)
     * @return Yüklenen model sayısı
     */
    @PostConstruct
    public void loadModelsOnStartup() {
        loadModels(null)
                .subscribe(
                        count -> log.info("{} adet model başarıyla yüklendi", count),
                        error -> log.error("Model yükleme sırasında hata oluştu: {}", error.getMessage()));
    }

    public Mono<Integer> loadModels(String configuredPath) {
        String path = configuredPath != null ? configuredPath : modelsPath;
        log.info("Modeller {} yolundan yükleniyor", path);

        try {
            Path filePath = Paths.get(path.replace("classpath:", ""));
            String jsonContent = Files.readString(filePath);

            List<Provider> providers = objectMapper.readValue(
                    jsonContent,
                    new TypeReference<List<Provider>>() {
                    });

            return Flux.fromIterable(providers)
                    .flatMap(provider -> {
                        List<AIModel> models = provider.getModels();
                        provider.setModels(null);

                        return providerRepository.save(provider)
                                .flatMapMany(savedProvider -> {
                                    if (models != null) {
                                        return Flux.fromIterable(models)
                                                .map(model -> {
                                                    // AIModel.provider alanını kullanarak providerId'yi tanımlama
                                                    model.setProvider(savedProvider.getName());
                                                    return model;
                                                })
                                                .flatMap(modelRepository::save);
                                    }
                                    return Flux.empty();
                                });
                    })
                    .count()
                    .map(Long::intValue);

        } catch (IOException e) {
            log.error("JSON model verisi yüklenirken hata: {}", e.getMessage());
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
