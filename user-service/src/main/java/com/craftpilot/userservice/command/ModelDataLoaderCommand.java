package com.craftpilot.userservice.command;

import com.craftpilot.userservice.service.ModelDataLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.load-models", havingValue = "true", matchIfMissing = true)
public class ModelDataLoaderCommand implements CommandLineRunner {

    private final ModelDataLoader modelDataLoader;
    
    @Value("${app.models.file:newmodels.json}")
    private String modelsFile;
    
    @Value("${app.models.timeout-seconds:60}")
    private int loadTimeoutSeconds;
    
    @Value("${app.models.load-on-startup:true}")
    private boolean loadOnStartup;

    @Override
    public void run(String... args) {
        if (!loadOnStartup) {
            log.info("CommandLineRunner: Model yükleme devre dışı (app.models.load-on-startup=false)");
            return;
        }
        
        // ApplicationReadyEvent ile çift yüklemeyi önlemek için durumu kontrol et
        Map<String, Object> status = modelDataLoader.getModelLoadingStatus();
        if ((boolean)status.get("loadInProgress")) {
            log.info("CommandLineRunner: Model yükleme işlemi zaten başlatıldı, devam eden işlem bekleniyor");
            return;
        }
        
        log.info("CommandLineRunner ile model yükleme başlatılıyor. '{}' dosyasından modeller yükleniyor", modelsFile);
        
        // Dosya yolunu belirle
        String jsonFilePath = args.length > 0 && args[0] != null && !args[0].isEmpty() ? 
                              args[0] : modelsFile;
        
        // jsonFilePath değerini düzelt, eğer "classpath:" ile başlamıyorsa ekle
        if (!jsonFilePath.startsWith("classpath:") && !jsonFilePath.startsWith("file:")) {
            jsonFilePath = "classpath:" + jsonFilePath;
        }
        
        try {
            // Modelleri yükle ve işlem tamamlanana kadar bekle
            Integer count = modelDataLoader.loadModelsFromFile(jsonFilePath)
                .timeout(Duration.ofSeconds(loadTimeoutSeconds))
                .doOnSuccess(loadedCount -> {
                    log.info("CommandLineRunner: AI model yükleme işlemi tamamlandı: {} model yüklendi", loadedCount);
                })
                .doOnError(error -> {
                    log.error("CommandLineRunner: AI model yükleme sırasında hata: {}", error.getMessage(), error);
                })
                .onErrorReturn(0) // Hata durumunda 0 döndür ama uygulamanın çalışmasını engelleme
                .block(Duration.ofSeconds(loadTimeoutSeconds + 10)); // Bloke etme süresi timeout + 10 saniye
            
            log.info("CommandLineRunner: Model yükleme işlemi sonucu: {} model yüklendi", count != null ? count : 0);
        } catch (Exception e) {
            log.error("CommandLineRunner: Model yükleme işlemi sırasında beklenmeyen hata: {}", e.getMessage(), e);
            // Uygulama başlangıcını engelleme
        }
    }
}
