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
@ConditionalOnProperty(name = "app.load-models", havingValue = "true")
public class ModelDataLoaderCommand implements CommandLineRunner {

    private final ModelDataLoader modelDataLoader;
    
    @Value("${app.models.file:newmodels.json}")
    private String modelsFile;

    @Override
    public void run(String... args) {
        log.info("AI modelleri yükleme işlemi başlıyor...");
        
        // Dosya yolunu belirle
        String jsonFilePath = args.length > 0 && args[0] != null && !args[0].isEmpty() ? 
                              args[0] : modelsFile;
        
        modelDataLoader.loadModelsFromJson(jsonFilePath)
            .timeout(Duration.ofMinutes(5))
            .doOnSuccess(count -> {
                log.info("AI model yükleme işlemi tamamlandı: {} model yüklendi", count);
            })
            .doOnError(error -> {
                log.error("AI model yükleme sırasında hata: {}", error.getMessage(), error);
            })
            .subscribe();
    }
}
