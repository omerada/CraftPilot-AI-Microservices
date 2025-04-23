package com.craftpilot.userservice.command;

import com.craftpilot.userservice.service.ModelDataLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Override
    public void run(String... args) {
        log.info("AI modelleri yükleme işlemi başlıyor...");
        
        String jsonFilePath = "c:\\Projects\\Craft-Pilot-Ai\\models.json";
        if (args.length > 0 && args[0] != null && !args[0].isEmpty()) {
            jsonFilePath = args[0];
        }
        
        modelDataLoader.loadModelsFromJson(jsonFilePath)
            .timeout(Duration.ofMinutes(5))
            .doOnSuccess(count -> {
                log.info("AI model yükleme işlemi tamamlandı: {} model yüklendi", count);
            })
            .doOnError(error -> {
                log.error("AI model yükleme sırasında hata: {}", error.getMessage(), error);
            })
            .block();
    }
}
