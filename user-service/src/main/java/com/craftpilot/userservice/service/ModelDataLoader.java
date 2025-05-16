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
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ModelDataLoader {

    private final ProviderRepository providerRepository;
    private final AIModelRepository modelRepository;
    private final ObjectMapper objectMapper;

    @Value("${spring.models.file:newmodels.json}")
    private String modelsFile;
    
    @Value("${spring.load-models:false}")
    private boolean loadModelsEnabled;

    /**
     * Yapılandırılabilir JSON dosya yolundan modelleri yükler ve MongoDB'ye
     * kaydeder - sadece app.load-models=true ise PostConstruct sırasında çalışır
     */
    @PostConstruct
    public void loadModelsOnStartup() {
        if (!loadModelsEnabled) {
            log.info("Otomatik model yükleme devre dışı bırakılmış (spring.load-models=false)");
            return;
        }
        
        log.info("Başlangıçta model yükleme etkin. '{}' dosyasından modeller yükleniyor", modelsFile);
        
        loadModels(modelsFile)
                .subscribe(
                        count -> log.info("{} adet model başarıyla yüklendi", count),
                        error -> log.error("Model yükleme sırasında hata oluştu: {}", error.getMessage()));
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
                
                log.info("Models loading from file system: {}", filePath.toAbsolutePath());
                String jsonContent = Files.readString(filePath);
                return processJsonContent(jsonContent);
            } else {
                // Classpath'ten yükle
                log.info("Models loading from classpath resource: {}", resource.getURL());
                try (InputStream is = resource.getInputStream()) {
                    String jsonContent = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    return processJsonContent(jsonContent);
                }
            }
        } catch (IOException e) {
            log.error("JSON model verisi yüklenirken hata: {}", e.getMessage());
            return Mono.error(e);
        }
    }
    
    private Mono<Integer> processJsonContent(String jsonContent) {
        try {
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
        } catch (Exception e) {
            log.error("JSON model verisi işlenirken hata: {}", e.getMessage());
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
