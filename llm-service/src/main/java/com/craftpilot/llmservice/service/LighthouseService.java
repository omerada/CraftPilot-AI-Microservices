package com.craftpilot.llmservice.service;

import com.craftpilot.llmservice.model.performance.PerformanceAnalysisResponse;
import com.craftpilot.llmservice.util.UrlValidator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class LighthouseService {
    private final ObjectMapper objectMapper;
    private final UrlValidator urlValidator;
    private final MeterRegistry meterRegistry;
    
    // Lighthouse komut yolunu özelleştirmek için bir değişken ekleyin
    // Bu, sisteminize özgü tam yolu belirtmek için kullanılabilir
    private String lighthousePath = "lighthouse"; // Varsayılan olarak PATH'ten alınan lighthouse
    
    // Lighthouse'un sistemde kurulu olup olmadığını kontrol eden metod
    private boolean isLighthouseInstalled() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("which", lighthousePath);
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            log.warn("Lighthouse installation check failed: {}", e.getMessage());
            return false;
        }
    }
    
    public Mono<PerformanceAnalysisResponse> analyzeSite(String url) {
        return Mono.fromCallable(() -> {
            log.info("Starting Lighthouse analysis for URL: {}", url);
            
            // URL'i doğrula
            urlValidator.validate(url);
            
            // Lighthouse'un kurulu olup olmadığını kontrol et
            if (!isLighthouseInstalled()) {
                log.error("Lighthouse is not installed or not found in PATH. Please install Lighthouse using npm install -g lighthouse");
                throw new RuntimeException("Lighthouse not installed or not found in system PATH");
            }
            
            long startTime = System.currentTimeMillis();
            
            try {
                // Lighthouse komut satırı aracını çağır
                ProcessBuilder processBuilder = new ProcessBuilder(
                        lighthousePath, 
                        url,
                        "--output=json", 
                        "--output-path=stdout",
                        "--chrome-flags=\"--headless --no-sandbox --disable-gpu\"",
                        "--only-categories=performance,accessibility,best-practices,seo"
                );
                
                processBuilder.redirectErrorStream(true);
                Process process = processBuilder.start();
                
                // 60 saniye timeout ayarla
                if (!process.waitFor(60, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                    throw new RuntimeException("Lighthouse analizi zaman aşımına uğradı");
                }

                // Sonuçları oku
                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line);
                    }
                }
                
                // Lighthouse hatası mı kontrol et
                int exitValue = process.exitValue();
                if (exitValue != 0) {
                    log.error("Lighthouse process exited with error code: {}, Output: {}", exitValue, output.toString());
                    throw new RuntimeException("Lighthouse analizi başarısız oldu - exit code: " + exitValue);
                }
                
                JsonNode lighthouseResult = objectMapper.readTree(output.toString());
                
                // Yanıtı oluştur
                Map<String, PerformanceAnalysisResponse.AuditResult> audits = new HashMap<>();
                JsonNode auditsNode = lighthouseResult.get("audits");
                
                // Önemli audit sonuçlarını çıkar
                for (String auditKey : new String[]{"first-contentful-paint", "largest-contentful-paint", 
                        "total-blocking-time", "cumulative-layout-shift", "speed-index"}) {
                    JsonNode audit = auditsNode.get(auditKey);
                    if (audit != null) {
                        audits.put(auditKey, PerformanceAnalysisResponse.AuditResult.builder()
                                .score(audit.get("score").asDouble())
                                .displayValue(audit.has("displayValue") ? audit.get("displayValue").asText() : "")
                                .description(audit.get("description").asText())
                                .build());
                    }
                }
                
                // Kategori sonuçlarını çıkar
                Map<String, PerformanceAnalysisResponse.CategoryResult> categories = new HashMap<>();
                JsonNode categoriesNode = lighthouseResult.get("categories");
                
                for (String categoryKey : new String[]{"performance", "accessibility", "best-practices", "seo"}) {
                    JsonNode category = categoriesNode.get(categoryKey);
                    if (category != null) {
                        categories.put(categoryKey, PerformanceAnalysisResponse.CategoryResult.builder()
                                .score(category.get("score").asDouble())
                                .build());
                    }
                }
                
                // Performans metriğini kaydet
                long duration = System.currentTimeMillis() - startTime;
                meterRegistry.timer("lighthouse.analysis.duration").record(duration, TimeUnit.MILLISECONDS);
                
                // Yanıtı oluştur
                return PerformanceAnalysisResponse.builder()
                        .id(UUID.randomUUID().toString())
                        .performance(categoriesNode.get("performance").get("score").asDouble())
                        .audits(audits)
                        .timestamp(Instant.now().toEpochMilli())
                        .url(url)
                        .categories(categories)
                        .build();
                
            } catch (IOException e) {
                log.error("Error executing Lighthouse: {}", e.getMessage());
                throw new RuntimeException("Lighthouse çalıştırılamadı: " + e.getMessage(), e);
            } catch (Exception e) {
                log.error("Unexpected error during Lighthouse analysis: {}", e.getMessage());
                throw new RuntimeException("Lighthouse analizi sırasında hata oluştu: " + e.getMessage(), e);
            }
        }).onErrorResume(e -> {
            // Hata durumunda en azından URL bilgisini içeren bir yanıt döndür
            log.error("Returning error response for URL {}: {}", url, e.getMessage());
            return Mono.just(PerformanceAnalysisResponse.builder()
                    .id(UUID.randomUUID().toString())
                    .performance(0.0)
                    .timestamp(Instant.now().toEpochMilli())
                    .url(url)
                    .error(e.getMessage())
                    .build());
        });
    }
    
    // Lighthouse yolunu ayarlamak için setter (özelleştirme için)
    public void setLighthousePath(String path) {
        this.lighthousePath = path;
    }
}
