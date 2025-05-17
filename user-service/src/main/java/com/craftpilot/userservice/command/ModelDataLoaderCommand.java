package com.craftpilot.userservice.command;

import com.craftpilot.userservice.service.ModelDataLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "spring.load-models", havingValue = "true", matchIfMissing = true)
public class ModelDataLoaderCommand implements CommandLineRunner {

    private final ModelDataLoader modelDataLoader;
    
    @Value("${spring.models.file:newmodels.json}")
    private String modelsFile;
    
    @Value("${spring.models.timeout:300}")
    private int loadTimeoutSeconds;

    @Override
    public void run(String... args) {
        log.info("CommandLineRunner ile model yükleme başlatılıyor. '{}' dosyasından modeller yükleniyor", modelsFile);
        
        // Dosya yolunu belirle
        String jsonFilePath = args.length > 0 && args[0] != null && !args[0].isEmpty() ? 
                              args[0] : modelsFile;
        
        try {
            // Modelleri yükle ve işlem tamamlanana kadar bekle
            Integer count = modelDataLoader.loadModelsFromJson(jsonFilePath)
                .timeout(Duration.ofSeconds(loadTimeoutSeconds))
                .doOnSuccess(loadedCount -> {
                    log.info("CommandLineRunner: AI model yükleme işlemi tamamlandı: {} model yüklendi", loadedCount);
                })
                .doOnError(error -> {
                    log.error("CommandLineRunner: AI model yükleme sırasında hata: {}", error.getMessage(), error);
                })
                .block(Duration.ofSeconds(loadTimeoutSeconds + 10)); // Bloke etme süresi timeout + 10 saniye
            
            log.info("CommandLineRunner: Model yükleme işlemi sonucu: {} model yüklendi", count != null ? count : 0);
        } catch (Exception e) {
            log.error("CommandLineRunner: Model yükleme işlemi sırasında beklenmeyen hata: {}", e.getMessage(), e);
        }
    }
}
