package com.craftpilot.lighthouseservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@Slf4j
public class LighthouseWorkerService {
    // Slf4j Logger'ın manuel tanımlanması (Lombok hatası durumunda kullanılır)
    private static final Logger logger = LoggerFactory.getLogger(LighthouseWorkerService.class);
    
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

    @Value("${lighthouse.cli.path:/usr/local/bin/lighthouse}")
    private String lighthousePath;

    @Value("${lighthouse.cli.timeout:300}")
    private int lighthouseTimeout;
    
    @Value("${lighthouse.cli.max-retries:2}")
    private int lighthouseMaxRetries;
    
    @Value("${lighthouse.cli.retry-delay:5000}")
    private int lighthouseRetryDelay;
    
    @Value("${lighthouse.cli.chrome-flags:--headless --no-sandbox --disable-gpu --disable-dev-shm-usage}")
    private String chromeFlags;
    
    @Value("${lighthouse.cli.categories.basic:performance}")
    private String basicCategories;
    
    @Value("${lighthouse.cli.categories.detailed:performance,accessibility,best-practices,seo}")
    private String detailedCategories;
    
    @Value("${lighthouse.temp.dir:/tmp/lighthouse}")
    private String tempDir; // Geçici dosyalar için dizin
    
    @Value("${lighthouse.cli.enable-error-reporting:true}")
    private boolean enableErrorReporting; // Hata raporlama etkin mi
    
    private final AtomicInteger activeWorkers = new AtomicInteger(0);
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Map<String, Long> processingJobs = new ConcurrentHashMap<>();
    
    public LighthouseWorkerService(ReactiveRedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        logger.info("Initializing Lighthouse worker service with {} workers", workerCount);
        // Worker'ları başlat
        startWorkers();
    }
    
    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down Lighthouse worker service");
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
            logger.debug("Active workers: {}/{}, starting more workers", activeWorkers.get(), workerCount);
            for (int i = activeWorkers.get(); i < workerCount; i++) {
                startWorker(i);
            }
        }
    }
    
    private void startWorker(int workerId) {
        final String workerName = "worker-" + workerId;
        
        logger.info("Starting Lighthouse worker: {}", workerName);
        activeWorkers.incrementAndGet();
        logger.info("{} started, active workers: {}/{}", workerName, activeWorkers.get(), workerCount);
        
        // Sürekli olarak kuyruktan işleri alan ve işleyen bir flux
        Flux.interval(Duration.ofMillis(pollInterval))
            .takeWhile(i -> running.get())
            .flatMap(i -> pollJob(workerName))
            .onErrorContinue((error, obj) -> {
                logger.error("{} error in job processing: {}", workerName, error.getMessage(), error);
            })
            .doFinally(signal -> {
                activeWorkers.decrementAndGet();
                logger.info("{} stopped, remaining active workers: {}", workerName, activeWorkers.get());
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
                            logger.info("{} received job {} for URL: {}", workerName, jobId, url);
                            return processJob(jobId, url, jobMap, workerName);
                        } else {
                            logger.warn("{} received invalid job: {}", workerName, jobMap);
                        }
                    } catch (Exception e) {
                        logger.error("{} error processing job: {}", workerName, e.getMessage(), e);
                    }
                } else if (job != null) {
                    logger.warn("{} received job of unexpected type: {}", workerName, 
                        job.getClass().getName());
                }
                return Mono.empty();
            })
            .onErrorResume(e -> {
                // TimeoutException'ları sessizce işle, diğer hataları logla
                if (!(e instanceof java.util.concurrent.TimeoutException)) {
                    logger.error("{} error polling jobs: {}", workerName, e.getMessage(), e);
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
        
        logger.info("{} processing job {} for URL: {} with analysisType: {}", workerName, jobId, url, analysisType);
        
        // İlk olarak job durumunu PROCESSING olarak güncelleyelim
        return updateJobStatus(jobId, "PROCESSING", null)
            .timeout(Duration.ofSeconds(5))
            .flatMap(success -> {
                if (Boolean.TRUE.equals(success)) {
                    logger.debug("{} updated job {} status to PROCESSING", workerName, jobId);
                } else {
                    logger.warn("{} failed to update job {} status to PROCESSING", workerName, jobId);
                }
                
                // Gerçek Lighthouse CLI çağrısı yap
                return runLighthouseAnalysis(url, analysisType, jobId)
                    .timeout("detailed".equals(analysisType) ? Duration.ofSeconds(120) : Duration.ofSeconds(60))
                    .flatMap(results -> saveJobResults(jobId, results)
                        .timeout(Duration.ofSeconds(5))
                        .flatMap(saved -> {
                            if (Boolean.TRUE.equals(saved)) {
                                logger.info("{} completed job {} successfully", workerName, jobId);
                                return Mono.empty();
                            } else {
                                logger.warn("{} failed to save results for job {}", workerName, jobId);
                                return updateJobStatus(jobId, "FAILED", "Failed to save results");
                            }
                        })
                        .onErrorResume(error -> {
                            logger.error("{} error saving results for job {}: {}", 
                                workerName, jobId, error.getMessage(), error);
                            return updateJobStatus(jobId, "FAILED", "Error saving results: " + error.getMessage());
                        })
                    )
                    .onErrorResume(error -> {
                        logger.error("{} error processing job {}: {}", 
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
        logger.debug("Updating job {} status to: {}", jobId, status);
        
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
                logger.error("Failed to update job status: {}", e.getMessage(), e);
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
                logger.error("Failed to save job results: {}", e.getMessage(), e);
                return Mono.just(false);
            });
    }
    
    // Gerçek Lighthouse CLI ile analiz yapan metod
    private Mono<Map<String, Object>> runLighthouseAnalysis(String url, String analysisType, String jobId) {
        return Mono.fromCallable(() -> executeCliCommand(url, analysisType, jobId))
            .retryWhen(Retry.backoff(lighthouseMaxRetries, Duration.ofMillis(lighthouseRetryDelay))
                .filter(this::shouldRetry)
                .doBeforeRetry(retrySignal -> {
                    logger.warn("Retrying Lighthouse analysis after error. Attempt: {}, URL: {}", 
                        retrySignal.totalRetries() + 1, url);
                }))
            .subscribeOn(Schedulers.boundedElastic())
            .onErrorResume(e -> {
                logger.error("Error running Lighthouse analysis: {}", e.getMessage(), e);
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("error", e.getMessage());
                errorResult.put("url", url);
                errorResult.put("analysisType", analysisType);
                errorResult.put("timestamp", System.currentTimeMillis());
                
                // Hata durumunda job durumunu FAILED olarak güncelle
                updateJobStatus(jobId, "FAILED", e.getMessage())
                    .subscribe(updated -> {
                        if (Boolean.TRUE.equals(updated)) {
                            logger.info("Updated job {} status to FAILED due to error", jobId);
                        }
                    });
                
                return Mono.just(errorResult);
            });
    }
    
    // Yeniden deneme yapılıp yapılmayacağını belirle
    private boolean shouldRetry(Throwable throwable) {
        if (throwable instanceof IOException) {
            return true;
        }
        
        String message = throwable.getMessage();
        return message != null && (
            message.contains("ECONNREFUSED") || 
            message.contains("ETIMEDOUT") || 
            message.contains("Connection refused") || 
            message.contains("Connection reset") ||
            message.contains("Chrome could not be found")
        );
    }
    
    // Lighthouse CLI komutunu çalıştırma
    private Map<String, Object> executeCliCommand(String url, String analysisType, String jobId) throws Exception {
        logger.info("Starting Lighthouse CLI analysis for URL: {} with type: {}", url, analysisType);
        
        // Lighthouse CLI ve gerekli programların varlığını kontrol et
        checkRequiredTools();
        
        // Geçici dizin varlığını kontrol et ve oluştur
        File tempDirFile = new File(tempDir);
        if (!tempDirFile.exists()) {
            logger.info("Creating temp directory: {}", tempDir);
            if (!tempDirFile.mkdirs()) {
                logger.warn("Failed to create temp directory: {}", tempDir);
            }
        }
        
        // Geçici dosya oluştur
        final String outputFilePath = tempDir + File.separator + "lighthouse-" + jobId + ".json";
        
        // Lighthouse CLI komut parametrelerini hazırla
        final List<String> command = buildLighthouseCommand(url, outputFilePath, analysisType);
        
        // Komutu çalıştır
        logger.info("Executing command: {}", String.join(" ", command));
        
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true); // Hata çıktılarını birleştir
        
        // İşlemi başlat
        final Process process = processBuilder.start();
        
        // Çıktıyı oku ve logla
        final StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                logger.debug("Lighthouse CLI output: {}", line);
            }
        }
        
        // İşlemin tamamlanmasını bekle
        final int exitCode = process.waitFor();
        
        // Eğer çıkış kodu başarısız ise
        if (exitCode != 0) {
            logger.error("Lighthouse CLI process exited with code: {}, Output: {}", exitCode, output.toString());
            throw new RuntimeException("Lighthouse CLI failed with exit code: " + exitCode + ". Details: " + 
                analyzeCliOutput(output.toString()));
        }
        
        // JSON çıktı dosyasını kontrol et
        final File outputFile = new File(outputFilePath);
        if (!outputFile.exists() || outputFile.length() == 0) {
            logger.error("Output file does not exist or is empty: {}, Command output: {}", outputFilePath, output);
            throw new RuntimeException("Lighthouse CLI failed to generate output file");
        }
        
        // JSON çıktıyı oku
        final String jsonContent = Files.readString(Path.of(outputFilePath));
        
        // JSON'ı Map'e dönüştür
        Map<String, Object> result;
        try {
            // Stream API kullanarak daha az bellek tüketimiyle JSON parse etme
            result = objectMapper.readValue(new File(outputFilePath), Map.class);
            
            // JSON dosyası çok büyükse ve bellek sorunları yaşanıyorsa,
            // aşağıdaki alternatifi kullanabilirsiniz:
            /*
            JsonFactory factory = objectMapper.getFactory();
            try (JsonParser parser = factory.createParser(new File(outputFilePath))) {
                result = objectMapper.readValue(parser, Map.class);
            }
            */
        } catch (Exception e) {
            logger.error("Failed to parse Lighthouse JSON: {}", e.getMessage());
            logger.debug("JSON parsing error details: ", e);
            
            // JSON dosyasının ilk 500 karakterini loglamak yerine dosya boyutunu logla
            logger.debug("JSON file size: {} bytes", Files.size(Path.of(outputFilePath)));
            throw new RuntimeException("Failed to parse Lighthouse output: " + e.getMessage(), e);
        }
        
        // Ek bilgileri ekle
        result.put("analysisType", analysisType);
        result.put("timestamp", System.currentTimeMillis());
        result.put("url", url);
        
        // Geçici dosyayı temizle
        try {
            Files.deleteIfExists(Path.of(outputFilePath));
        } catch (IOException e) {
            logger.warn("Could not delete temporary file: {}", outputFilePath);
        }
        
        return result;
    }
    
    // Lighthouse komutu oluşturma
    private List<String> buildLighthouseCommand(String url, String outputFilePath, String analysisType) {
        final List<String> command = new ArrayList<>();
        
        // Doğrudan lighthouse komutunu kullan (npx olmadan)
        command.add(lighthousePath);
        command.add(url);
        command.add("--output=json");
        command.add("--output-path=" + outputFilePath);
        
        // Chrome flags ekle
        command.add("--chrome-flags=" + chromeFlags);
        
        // Analiz tipini normalize et
        final String normalizedAnalysisType = (analysisType == null || analysisType.trim().isEmpty()) 
                                           ? "basic" 
                                           : analysisType.trim().toLowerCase();
        
        // Analiz tipine göre ek parametreler ekle
        switch (normalizedAnalysisType) {
            case "detailed":
                command.add("--only-categories=" + detailedCategories);
                command.add("--throttling-method=devtools");
                break;
            case "basic":
            default:
                command.add("--only-categories=" + basicCategories);
                command.add("--throttling-method=simulate");
                command.add("--max-wait-for-load=30000");  // Daha kısa bekleme süresi
                break;
        }
        
        return command;
    }
    
    // Gerekli araçları daha kapsamlı kontrol et
    private void checkRequiredTools() throws Exception {
        logger.info("Checking required tools");
        
        // Java kontrolü
        try {
            Process process = new ProcessBuilder("java", "-version")
                .redirectErrorStream(true)
                .start();
            
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                logger.error("Java check failed with exit code: {}, Output: {}", exitCode, output);
            } else {
                logger.info("Java check passed: {}", output.toString().trim());
            }
        } catch (Exception e) {
            logger.error("Error checking Java: {}", e.getMessage());
        }
        
        // Tarayıcı yolları - Linux dağıtımlarında tipik olarak bulunanlar
        String[] possibleBrowserPaths = {
            "/usr/bin/chromium-browser",
            "/usr/bin/chromium", 
            "/usr/bin/google-chrome",
            "/usr/local/bin/chromium",
            "/snap/bin/chromium"
        };
        
        try {
            // Tarayıcı kontrolü
            boolean browserFound = false;
            for (String path : possibleBrowserPaths) {
                File browserFile = new File(path);
                if (browserFile.exists() && browserFile.canExecute()) {
                    System.setProperty("CHROME_PATH", path);
                    logger.info("Using Chrome/Chromium at: {}", path);
                    browserFound = true;
                    break;
                }
            }
            
            if (!browserFound) {
                logger.warn("No Chrome/Chromium browser found in standard locations");
                // Sistem PATH'inde arama yap
                try {
                    Process process = new ProcessBuilder("which", "chromium")
                        .redirectErrorStream(true)
                        .start();
                    
                    StringBuilder output = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            output.append(line);
                        }
                    }
                    
                    int exitCode = process.waitFor();
                    if (exitCode == 0) {
                        String chromePath = output.toString().trim();
                        System.setProperty("CHROME_PATH", chromePath);
                        logger.info("Found Chromium in PATH: {}", chromePath);
                        browserFound = true;
                    }
                } catch (Exception e) {
                    logger.warn("Error while searching for Chromium in PATH: {}", e.getMessage());
                }
                
                if (!browserFound) {
                    logger.warn("Using default system Chrome");
                }
            }
            
            // Lighthouse kontrolü
            try {
                Process process = new ProcessBuilder("which", lighthousePath.split("\\s+")[0])
                    .redirectErrorStream(true)
                    .start();
                
                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line);
                    }
                }
                
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    logger.warn("Lighthouse not found at: {} (exit code: {})", lighthousePath, exitCode);
                    
                    // NPX kullanarak lighthouse'u çalıştırmayı dene
                    logger.info("Trying to use npx lighthouse");
                    lighthousePath = "npx lighthouse";
                    
                    // NPX kontrolü
                    Process npxProcess = new ProcessBuilder("which", "npx")
                        .redirectErrorStream(true)
                        .start();
                    
                    StringBuilder npxOutput = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(npxProcess.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            npxOutput.append(line);
                        }
                    }
                    
                    int npxExitCode = npxProcess.waitFor();
                    if (npxExitCode == 0) {
                        logger.info("Will use NPX to run lighthouse: {}", npxOutput.toString().trim());
                    } else {
                        logger.warn("NPX not found in PATH, lighthouse may not work properly");
                    }
                } else {
                    logger.info("Lighthouse found: {}", output.toString().trim());
                }
            } catch (Exception e) {
                logger.warn("Could not check lighthouse path: {}", e.getMessage());
                // Continue anyway
            }
        } catch (Exception e) {
            logger.warn("Error during tool check: {}", e.getMessage());
            // Just log and continue - don't throw exceptions
        }
    }

    // CLI çıktısını analiz eder ve hata mesajlarını çıkarır
    private String analyzeCliOutput(String output) {
        if (output == null || output.isEmpty()) {
            return "Unknown error - no output";
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
            StringBuilder lastLines = new StringBuilder("Last output lines: ");
            for (int i = Math.max(0, lines.length - 5); i < lines.length; i++) {
                if (!lines[i].trim().isEmpty()) {
                    lastLines.append(lines[i].trim()).append("; ");
                }
            }
            return lastLines.toString();
        }
        
        return "Unknown error occurred";
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
                logger.warn("Job {} appears to be stuck (processing for {} ms)", jobId, now - startTime);
                updateJobStatus(jobId, "FAILED", "Job timed out after " + (now - startTime) / 1000 + " seconds")
                    .subscribe(
                        updated -> {
                            if (Boolean.TRUE.equals(updated)) {
                                logger.info("Marked stuck job {} as failed", jobId);
                                processingJobs.remove(jobId);
                            }
                        },
                        error -> logger.error("Failed to mark job {} as failed: {}", jobId, error.getMessage())
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
            logger.debug("Checking and starting workers for queue processing");
            for (int i = activeWorkers.get(); i < workerCount; i++) {
                startWorker(i);
            }
        }
    }
}
