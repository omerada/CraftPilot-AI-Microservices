package com.craftpilot.lighthouseservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class LighthouseWorkerService {
    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${lighthouse.queue.name}")
    private String queueName;
    
    @Value("${lighthouse.results.prefix}")
    private String resultsPrefix;
    
    @Value("${lighthouse.worker.count:3}")
    private int workerCount;
    
    @Value("${lighthouse.worker.poll-interval:1000}")
    private int pollInterval; // ms

    @Value("${lighthouse.cli.path:lighthouse}")
    private String lighthousePath; // Lighthouse CLI yolunu yapılandırmadan ayarlama imkanı
    
    @Value("${lighthouse.temp.dir:/tmp}")
    private String tempDir; // Geçici dosyalar için dizin
    
    @Value("${lighthouse.cli.max-retries:2}")
    private int lighthouseMaxRetries; // Lighthouse CLI yeniden deneme sayısı
    
    @Value("${lighthouse.cli.retry-delay:5000}")
    private int lighthouseRetryDelay; // Lighthouse CLI yeniden deneme gecikmesi (ms)
    
    @Value("${lighthouse.cli.enable-error-reporting:true}")
    private boolean enableErrorReporting; // Hata raporlama etkin mi
    
    private final AtomicInteger activeWorkers = new AtomicInteger(0);
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Map<String, Long> processingJobs = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() {
        log.info("Initializing Lighthouse worker service with {} workers", workerCount);
        // Worker'ları başlat
        startWorkers();
    }
    
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down Lighthouse worker service");
        running.set(false);
    }
    
    public void startWorkers() {
        // İlk worker'ları başlat
        for (int i = 0; i < workerCount; i++) {
            if (running.get()) {
                startWorker(i);
            }
        }
    }
    
    @Scheduled(fixedDelayString = "${lighthouse.worker.check-interval:10000}")
    public void checkAndRestartWorkers() {
        // Worker sayısını kontrol et ve gerekirse yeni worker'lar başlat
        if (activeWorkers.get() < workerCount && running.get()) {
            log.debug("Active workers: {}/{}, starting more workers", activeWorkers.get(), workerCount);
            for (int i = activeWorkers.get(); i < workerCount; i++) {
                startWorker(i);
            }
        }
    }
    
    private void startWorker(int workerId) {
        final String workerName = "worker-" + workerId;
        
        log.info("Starting Lighthouse worker: {}", workerName);
        activeWorkers.incrementAndGet();
        log.info("{} started, active workers: {}/{}", workerName, activeWorkers.get(), workerCount);
        
        // Sürekli olarak kuyruktan işleri alan ve işleyen bir flux
        Flux.interval(Duration.ofMillis(pollInterval))
            .takeWhile(i -> running.get())
            .flatMap(i -> pollJob(workerName))
            .onErrorContinue((error, obj) -> {
                log.error("{} error in job processing: {}", workerName, error.getMessage(), error);
            })
            .doFinally(signal -> {
                activeWorkers.decrementAndGet();
                log.info("{} stopped, remaining active workers: {}", workerName, activeWorkers.get());
            })
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe();
    }
    
    private Mono<Void> pollJob(String workerName) {
        // LPOP komutunu kullanarak kuyruktan bir iş al
        return redisTemplate.opsForList().leftPop(queueName)
            .timeout(Duration.ofSeconds(3))
            .flatMap(job -> {
                if (job instanceof Map) {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> jobMap = (Map<String, Object>) job;
                        String jobId = (String) jobMap.get("id");
                        String url = (String) jobMap.get("url");
                        
                        if (jobId != null && url != null) {
                            log.info("{} received job {} for URL: {}", workerName, jobId, url);
                            return processJob(jobId, url, jobMap, workerName);
                        } else {
                            log.warn("{} received invalid job: {}", workerName, jobMap);
                        }
                    } catch (Exception e) {
                        log.error("{} error processing job: {}", workerName, e.getMessage(), e);
                    }
                } else if (job != null) {
                    log.warn("{} received job of unexpected type: {}", workerName, 
                        job.getClass().getName());
                }
                return Mono.empty();
            })
            .onErrorResume(e -> {
                // TimeoutException'ları sessizce işle, diğer hataları logla
                if (!(e instanceof java.util.concurrent.TimeoutException)) {
                    log.error("{} error polling jobs: {}", workerName, e.getMessage(), e);
                }
                return Mono.empty();
            })
            .then();
    }
    
    private Mono<Void> processJob(String jobId, String url, Map<String, Object> jobMap, String workerName) {
        // İşleme başlama zamanını kaydet
        processingJobs.put(jobId, System.currentTimeMillis());
        
        // Analiz tipini belirle (varsayılan olarak "basic")
        final String analysisType = determineAnalysisType(jobMap);
        
        log.info("{} processing job {} for URL: {} with analysisType: {}", workerName, jobId, url, analysisType);
        
        // İlk olarak job durumunu PROCESSING olarak güncelleyelim
        return updateJobStatus(jobId, "PROCESSING", null)
            .timeout(Duration.ofSeconds(5))
            .flatMap(success -> {
                if (Boolean.TRUE.equals(success)) {
                    log.debug("{} updated job {} status to PROCESSING", workerName, jobId);
                } else {
                    log.warn("{} failed to update job {} status to PROCESSING", workerName, jobId);
                }
                
                // Gerçek Lighthouse CLI çağrısı yap
                return runLighthouseAnalysis(url, analysisType, jobId)
                    .timeout("detailed".equals(analysisType) ? Duration.ofSeconds(120) : Duration.ofSeconds(60))
                    .flatMap(results -> saveJobResults(jobId, results)
                        .timeout(Duration.ofSeconds(5))
                        .flatMap(saved -> {
                            if (Boolean.TRUE.equals(saved)) {
                                log.info("{} completed job {} successfully", workerName, jobId);
                                return Mono.empty();
                            } else {
                                log.warn("{} failed to save results for job {}", workerName, jobId);
                                return updateJobStatus(jobId, "FAILED", "Failed to save results");
                            }
                        })
                        .onErrorResume(error -> {
                            log.error("{} error saving results for job {}: {}", 
                                workerName, jobId, error.getMessage(), error);
                            return updateJobStatus(jobId, "FAILED", "Error saving results: " + error.getMessage());
                        })
                    )
                    .onErrorResume(error -> {
                        log.error("{} error processing job {}: {}", 
                            workerName, jobId, error.getMessage(), error);
                        return updateJobStatus(jobId, "FAILED", "Processing error: " + error.getMessage());
                    })
                    .doFinally(signal -> processingJobs.remove(jobId));
            })
            .then();
    }
    
    /**
     * Job map'inden analiz tipini belirleyen yardımcı metod
     */
    private String determineAnalysisType(Map<String, Object> jobMap) {
        if (jobMap.containsKey("options") && ((Map<String, Object>)jobMap.get("options")).containsKey("analysisType")) {
            return (String)((Map<String, Object>)jobMap.get("options")).get("analysisType");
        }
        return "basic";
    }
    
    private Mono<Boolean> updateJobStatus(String jobId, String status, String errorMessage) {
        log.debug("Updating job {} status to: {}", jobId, status);
        
        // Hata varsa durumu FAILED olarak güncelle
        String finalStatus = status;
        if (errorMessage != null && !errorMessage.isEmpty() && "COMPLETED".equals(status)) {
            finalStatus = "FAILED";
        }
        
        Map<String, Object> jobStatus = Map.of(
            "jobId", jobId,
            "complete", "COMPLETED".equals(finalStatus) || "FAILED".equals(finalStatus),
            "status", finalStatus,
            "error", errorMessage != null ? errorMessage : "",
            "timestamp", System.currentTimeMillis()
        );
            
        return redisTemplate.opsForValue()
            .set(resultsPrefix + "status:" + jobId, jobStatus, Duration.ofMinutes(30))
            .timeout(Duration.ofSeconds(3))
            .onErrorResume(e -> {
                log.error("Failed to update job status: {}", e.getMessage(), e);
                return Mono.just(false);
            });
    }
    
    private Mono<Boolean> saveJobResults(String jobId, Map<String, Object> results) {
        // Önce job sonuçlarını kaydet
        return redisTemplate.opsForValue()
            .set(resultsPrefix + jobId, results, Duration.ofDays(1))
            .then(updateJobStatus(jobId, "COMPLETED", null))
            .timeout(Duration.ofSeconds(5))
            .onErrorResume(e -> {
                log.error("Failed to save job results: {}", e.getMessage(), e);
                return Mono.just(false);
            });
    }
    
    // Gerçek Lighthouse CLI ile analiz yapan metod
    private Mono<Map<String, Object>> runLighthouseAnalysis(String url, String analysisType, String jobId) {
        return Mono.fromCallable(() -> executeCliCommand(url, analysisType, jobId))
            .retryWhen(Retry.backoff(lighthouseMaxRetries, Duration.ofMillis(lighthouseRetryDelay))
                .filter(e -> shouldRetry(e))
                .doBeforeRetry(retrySignal -> {
                    log.warn("Retrying Lighthouse analysis after error. Attempt: {}, URL: {}", 
                        retrySignal.totalRetries() + 1, url);
                }))
            .subscribeOn(Schedulers.boundedElastic())
            .onErrorResume(e -> {
                log.error("Error running Lighthouse analysis: {}", e.getMessage(), e);
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("error", e.getMessage());
                errorResult.put("url", url);
                errorResult.put("analysisType", analysisType);
                errorResult.put("timestamp", System.currentTimeMillis());
                
                // Hata durumunda job durumunu FAILED olarak güncelle
                updateJobStatus(jobId, "FAILED", e.getMessage())
                    .subscribe(updated -> {
                        if (Boolean.TRUE.equals(updated)) {
                            log.info("Updated job {} status to FAILED due to error", jobId);
                        }
                    });
                
                return Mono.just(errorResult);
            });
    }
    
    // Lighthouse CLI komutunu çalıştırma
    private Map<String, Object> executeCliCommand(String url, String analysisType, String jobId) throws Exception {
        log.info("Starting Lighthouse CLI analysis for URL: {} with type: {}", url, analysisType);
        
        // Lighthouse CLI ve gerekli programların varlığını kontrol et
        checkRequiredTools();
        
        // Geçici dizin varlığını kontrol et ve oluştur
        File tempDirFile = new File(tempDir);
        if (!tempDirFile.exists()) {
            log.info("Creating temp directory: {}", tempDir);
            if (!tempDirFile.mkdirs()) {
                log.warn("Failed to create temp directory: {}", tempDir);
            }
        }
        
        // Geçici dosya oluştur
        final String outputFilePath = tempDir + File.separator + "lighthouse-" + jobId + ".json";
        
        // Lighthouse CLI komut parametrelerini hazırla
        final List<String> command = buildLighthouseCommand(url, outputFilePath, analysisType);
        
        // Komutu çalıştır - ProcessBuilder yerine daha direkt erişim için Runtime kullanıyoruz
        log.info("Executing command: {}", String.join(" ", command));
        
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true); // Hata çıktılarını birleştir
        
        // NODE_PATH çevre değişkenini ayarlayarak modul bulma sorunlarını çöz
        Map<String, String> env = processBuilder.environment();
        String nodePath = env.getOrDefault("NODE_PATH", "");
        // Node modüllerinin bulunabileceği standart konumlar ekle
        env.put("NODE_PATH", nodePath + ":/usr/local/lib/node_modules:/usr/lib/node_modules");
        
        // İşlemi başlat
        final Process process = processBuilder.start();
        
        // Çıktıyı oku ve logla
        final StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                log.info("Lighthouse CLI output: {}", line);
            }
        }
        
        // İşlemin tamamlanmasını bekle
        final int exitCode = process.waitFor();
        
        // Eğer çıkış kodu başarısız ise
        if (exitCode != 0) {
            log.error("Lighthouse CLI process exited with code: {}, Output: {}", exitCode, output.toString());
            
            // Çıktıdaki hata mesajlarını incele
            String errorDetails = analyzeCliOutput(output.toString());
            throw new RuntimeException("Lighthouse CLI failed with exit code: " + exitCode + 
                (errorDetails != null ? ". Details: " + errorDetails : ""));
        }
        
        // JSON çıktı dosyasını kontrol et
        final File outputFile = new File(outputFilePath);
        if (!outputFile.exists() || outputFile.length() == 0) {
            log.error("Output file does not exist or is empty: {}, Command output: {}", outputFilePath, output);
            throw new RuntimeException("Lighthouse CLI failed to generate output file");
        }
        
        // JSON çıktıyı oku
        final String jsonContent = Files.readString(Path.of(outputFilePath));
        
        // JSON'ı Map'e dönüştür
        Map<String, Object> result;
        try {
            result = objectMapper.readValue(jsonContent, Map.class);
        } catch (Exception e) {
            log.error("Failed to parse Lighthouse JSON: {}", e.getMessage());
            log.debug("JSON content: {}", jsonContent.substring(0, Math.min(500, jsonContent.length())));
            throw new RuntimeException("Failed to parse Lighthouse output: " + e.getMessage());
        }
        
        // Ek bilgileri ekle
        result.put("analysisType", analysisType);
        result.put("timestamp", System.currentTimeMillis());
        result.put("url", url);
        
        // Geçici dosyayı temizle
        try {
            Files.deleteIfExists(Path.of(outputFilePath));
        } catch (IOException e) {
            log.warn("Could not delete temporary file: {}", outputFilePath);
        }
        
        return result;
    }
    
    // Lighthouse komutu oluşturma
    private List<String> buildLighthouseCommand(String url, String outputFilePath, String analysisType) {
        final List<String> command = new ArrayList<>();
        
        // Lighthouse CLI için NPX kullan - NPX modülü bulamama sorunlarını çözebilir
        command.add("npx");
        command.add(lighthousePath);
        command.add(url);
        command.add("--output=json");
        command.add("--output-path=" + outputFilePath);
        
        // Chrome flags düzeltildi - çift tırnak sorunlarını kaldır
        command.add("--chrome-flags=--headless --no-sandbox --disable-gpu --disable-dev-shm-usage");
        
        // Analiz tipini normalize et
        final String normalizedAnalysisType = (analysisType == null || analysisType.trim().isEmpty()) 
                                           ? "basic" 
                                           : analysisType.trim().toLowerCase();
        
        // Analiz tipine göre ek parametreler ekle
        switch (normalizedAnalysisType) {
            case "detailed":
                // Detaylı analiz için tüm kategorileri kontrol et
                command.add("--throttling.cpuSlowdownMultiplier=4");
                command.add("--throttling-method=devtools");
                break;
            case "basic":
            default:
                // Temel analiz için sadece performans kategorisini kontrol et
                command.add("--only-categories=performance");
                command.add("--throttling-method=simulate");
                command.add("--max-wait-for-load=30000");  // Daha kısa bekleme süresi
                break;
        }
        
        return command;
    }
    
    // Gerekli araçları kontrol et
    private void checkRequiredTools() throws Exception {
        // Lighthouse varlığını kontrol et - npx ile çalıştırılacak
        Process npmCheck = Runtime.getRuntime().exec("which npm");
        int npmExitCode = npmCheck.waitFor();
        
        if (npmExitCode != 0) {
            log.error("npm not found on system path. Lighthouse CLI cannot run.");
            throw new RuntimeException("Required tool npm is not installed or not accessible");
        }
        
        // Chrome/Chromium varlığını kontrol et
        String[] browsers = {"chromium", "google-chrome", "chrome"};
        boolean foundBrowser = false;
        
        for (String browser : browsers) {
            Process browserCheck = Runtime.getRuntime().exec("which " + browser);
            if (browserCheck.waitFor() == 0) {
                log.info("Found browser: {}", browser);
                foundBrowser = true;
                break;
            }
        }
        
        if (!foundBrowser) {
            log.error("No Chrome/Chromium browser found on system path");
            throw new RuntimeException("Required browser (Chrome/Chromium) is not installed or not accessible");
        }
    }

    // CLI çıktısını analiz eder ve hata mesajlarını çıkarır
    private String analyzeCliOutput(String output) {
        if (output == null || output.isEmpty()) {
            return null;
        }
        
        // Node.js/NPM modül hatalarını tespit et
        if (output.contains("Cannot find module")) {
            return "Node.js module not found: " + extractModuleName(output);
        }
        
        if (output.contains("no such file or directory") || output.contains("not found")) {
            return "Lighthouse CLI or required dependency not found";
        }
        
        if (output.contains("chrome not found") || output.contains("Chrome could not be found")) {
            return "Chrome browser could not be found";
        }
        
        if (output.contains("EACCES") || output.contains("permission denied")) {
            return "Permission denied when trying to access resources";
        }
        
        if (output.contains("ERR_CONNECTION_REFUSED") || output.contains("ERR_NAME_NOT_RESOLVED")) {
            return "Failed to connect to the target URL";
        }
        
        if (output.contains("TimeoutError") || output.contains("Navigation Timeout Exceeded")) {
            return "Page load timeout - the target URL took too long to respond";
        }
        
        // Son 5 satır genellikle en önemli hata mesajlarını içerir
        String[] lines = output.split("\n");
        if (lines.length > 0) {
            StringBuilder lastLines = new StringBuilder("Last lines: ");
            for (int i = Math.max(0, lines.length - 5); i < lines.length; i++) {
                if (!lines[i].trim().isEmpty()) {
                    lastLines.append(lines[i].trim()).append("; ");
                }
            }
            return lastLines.toString();
        }
        
        return null;
    }
    
    // Modül adı çıkarma yardımcı metodu
    private String extractModuleName(String output) {
        // "Cannot find module 'xyz'" formatındaki hatalardan modül adını çıkarır
        int startIdx = output.indexOf("Cannot find module '");
        if (startIdx >= 0) {
            startIdx += "Cannot find module '".length();
            int endIdx = output.indexOf("'", startIdx);
            if (endIdx > startIdx) {
                return output.substring(startIdx, endIdx);
            }
        }
        return "unknown module";
    }
    
    // Hangi hataların yeniden denenmesi gerektiğini belirle
    private boolean shouldRetry(Throwable error) {
        String message = error.getMessage() != null ? error.getMessage().toLowerCase() : "";
        // Chrome başlatma hatalarını yeniden dene,
        // Ağ hatalarını yeniden dene, 
        // Ancak çıktı dosyası yazma izni gibi kalıcı hataları deneme
        return message.contains("chrome") || 
               message.contains("connection") || 
               message.contains("timeout") ||
               message.contains("refused");
    }
    
    // Aktif çalışan worker sayısını döndürür - sağlık kontrolü için kullanılır
    public int getActiveWorkerCount() {
        return activeWorkers.get();
    }
    
    // Takılmış işleri kontrol et
    @Scheduled(fixedRate = 60000) // her dakika
    public void checkStuckJobs() {
        final long stuckThresholdMs = 5 * 60 * 1000; // 5 dakika
        final long now = System.currentTimeMillis();
        
        processingJobs.forEach((jobId, startTime) -> {
            if (now - startTime > stuckThresholdMs) {
                log.warn("Job {} appears to be stuck (processing for {} ms)", jobId, now - startTime);
                updateJobStatus(jobId, "FAILED", "Job timed out after " + (now - startTime) / 1000 + " seconds")
                    .subscribe(
                        updated -> {
                            if (Boolean.TRUE.equals(updated)) {
                                log.info("Marked stuck job {} as failed", jobId);
                                processingJobs.remove(jobId);
                            }
                        },
                        error -> log.error("Failed to mark job {} as failed: {}", jobId, error.getMessage())
                    );
            }
        });
    }
    
    /**
     * Kuyruk durumunu kontrol eder ve gerekirse yeni işçiler başlatır
     */
    public void checkAndProcessQueue() {
        // Worker sayısını kontrol et ve gerekirse yeni worker'lar başlat
        if (activeWorkers.get() < workerCount && running.get()) {
            log.debug("Checking and starting workers for queue processing");
            for (int i = activeWorkers.get(); i < workerCount; i++) {
                startWorker(i);
            }
        }
    }
}
