package com.craftpilot.llmservice.service;

import com.craftpilot.llmservice.cache.PerformanceAnalysisCache;
import com.craftpilot.llmservice.model.AIRequest;
import com.craftpilot.llmservice.model.AIResponse;
import com.craftpilot.llmservice.model.StreamResponse;
import com.craftpilot.llmservice.model.performance.*;
import com.craftpilot.llmservice.repository.PerformanceAnalysisRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import com.craftpilot.llmservice.model.performance.PerformanceHistoryResponse.PerformanceHistoryEntry;

@Service
@RequiredArgsConstructor
@Slf4j
public class PerformanceService {
    private final PerformanceAnalysisRepository performanceAnalysisRepository;
    private final PerformanceAnalysisCache performanceAnalysisCache;
    private final PromptService promptService;
    private final MeterRegistry meterRegistry;
    private final LLMService llmService;
    private final ObjectMapper objectMapper;

    @Value("${lighthouse.service.url:http://lighthouse-service:8085}")
    private String lighthouseServiceUrl;

    private final WebClient webClient = WebClient.builder().build();

    public Mono<PerformanceAnalysisResponse> analyzeWebsite(PerformanceAnalysisRequest request) {
        // URL'i önbellekte ara
        return performanceAnalysisCache.getAnalysisResult(request.getUrl())
                .switchIfEmpty(
                    // Önbellekte yoksa lighthouse-service'e istek gönder
                    webClient.post()
                        .uri(lighthouseServiceUrl + "/api/v1/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(Map.of("url", request.getUrl()))
                        .retrieve()
                        .bodyToMono(Map.class)
                        .flatMap(response -> {
                            String jobId = (String) response.get("jobId");
                            log.info("Analysis job queued with ID: {}", jobId);
                            
                            // Job sonucunu polling ile bekle
                            return pollForResultsWithRetry(jobId);
                        })
                        .flatMap(response -> {
                            // Veritabanına kaydet
                            return performanceAnalysisRepository.save(response)
                                .doOnSuccess(saved -> {
                                    // Önbelleğe ekle
                                    performanceAnalysisCache.cacheAnalysisResult(request.getUrl(), saved);
                                    
                                    // Metrikleri kaydet
                                    meterRegistry.counter("performance.analysis.completed").increment();
                                });
                        })
                );
    }
    
    private Mono<PerformanceAnalysisResponse> pollForResultsWithRetry(String jobId) {
        // Maksimum deneme sayısı ve bekleme süresi
        final int MAX_RETRIES = 10;
        final Duration INITIAL_BACKOFF = Duration.ofSeconds(2);
        
        return Mono.defer(() -> pollForResults(jobId))
            .retryWhen(Retry.backoff(MAX_RETRIES, INITIAL_BACKOFF)
                .filter(e -> e instanceof RuntimeException && 
                        e.getMessage() != null && 
                        e.getMessage().contains("Job not completed yet"))
                .maxBackoff(Duration.ofSeconds(20))
                .doAfterRetry(retrySignal -> 
                    log.info("Retrying poll for job {}, attempt {}", 
                        jobId, retrySignal.totalRetries() + 1))
            );
    }
    
    private Mono<PerformanceAnalysisResponse> pollForResults(String jobId) {
        return webClient.get()
            .uri(lighthouseServiceUrl + "/api/v1/report/" + jobId)
            .retrieve()
            .bodyToMono(Map.class)
            .flatMap(response -> {
                Boolean isComplete = (Boolean) response.getOrDefault("complete", false);
                
                if (Boolean.TRUE.equals(isComplete) && response.get("data") != null) {
                    // Sonucu PerformanceAnalysisResponse'a dönüştür
                    Object data = response.get("data");
                    return Mono.just(objectMapper.convertValue(data, PerformanceAnalysisResponse.class));
                } else {
                    // İş henüz tamamlanmamış, hata döndür
                    String status = (String) response.getOrDefault("status", "unknown");
                    String error = (String) response.getOrDefault("error", "Job not completed yet");
                    return Mono.error(new RuntimeException("Job status: " + status + ", Error: " + error));
                }
            });
    }
    
    /**
     * Performans analiz verilerine dayanarak iyileştirme önerileri üretir
     * Bu metod doğrudan LLMService'i kullanarak OpenRouter AI entegrasyonu yapar
     */
    public Mono<SuggestionsResponse> generateSuggestions(SuggestionsRequest request) {
        String requestId = UUID.randomUUID().toString();
        
        AIRequest aiRequest = new AIRequest();
        aiRequest.setRequestId(requestId);
        aiRequest.setUserId(request.getUserId());
        // Default model kullanım örneği - gemini-2.0-flash-lite-001 gibi hızlı bir model
        aiRequest.setModel(request.getModel() != null ? request.getModel() : "google/gemini-2.0-flash-lite-001");
        // Varsayılan token limiti ve sıcaklık değerleri
        aiRequest.setMaxTokens(request.getMaxTokens() != null ? request.getMaxTokens() : 2000);
        aiRequest.setTemperature(request.getTemperature() != null ? request.getTemperature() : 0.7);
        aiRequest.setLanguage(request.getLanguage() != null ? request.getLanguage() : "tr"); // Varsayılan olarak Türkçe yanıt
        
        try {
            // Analiz verilerini JSON string'e dönüştür
            String analysisJson = objectMapper.writeValueAsString(request.getAnalysisData());
            
            // Performans önerileri için system prompt
            String systemPrompt = "Sen bir web performans optimizasyonu uzmanısın. " +
                    "Verilen Lighthouse performans verilerini analiz et ve spesifik iyileştirmeler öner. " +
                    "Her sorun için şunları belirt: problem açıklaması, önem derecesi (kritik/önemli/düşük), çözüm, " +
                    "kod örneği, faydalı kaynaklar ve uygulama zorluğu (kolay/orta/zor). " +
                    "Yanıtını geçerli bir JSON dizisi olarak formatla.";
            
            aiRequest.setSystemPrompt(systemPrompt);
            
            // Analiz verileriyle prompt oluştur
            String prompt = "Bu Lighthouse performans analiz verilerine dayanarak, " +
                    "web sitesi performansını iyileştirmek için detaylı öneriler sun. Önce en etkili değişikliklere odaklan:\n\n" +
                    analysisJson + "\n\n" +
                    "Yanıtını her biri için şu özelliklere sahip bir JSON dizisi olarak formatla: " +
                    "problem, severity, solution, codeExample, resources (URL'ler dizisi), implementationDifficulty";
            
            aiRequest.setPrompt(prompt);
            
            log.info("OpenRouter AI'ya performans iyileştirme önerileri için istek gönderiliyor");
            return llmService.processChatCompletion(aiRequest)
                    .map(aiResponse -> {
                        SuggestionsResponse response = new SuggestionsResponse();
                        response.setContent(aiResponse.getResponse());
                        return response;
                    })
                    .doOnSuccess(resp -> log.info("Performans iyileştirme önerileri başarıyla alındı"))
                    .doOnError(error -> log.error("Öneri oluşturma hatası: {}", error.getMessage(), error));
                    
        } catch (JsonProcessingException e) {
            log.error("Analiz verileri işlenirken hata: {}", e.getMessage());
            return Mono.error(e);
        }
    }
    
    /**
     * Performans iyileştirme önerilerini gerçek zamanlı olarak stream eder
     * Bu metod OpenRouter stream API'sini kullanarak yanıtları parça parça gönderir
     */
    public Flux<StreamSuggestionsResponse> streamSuggestions(StreamSuggestionsRequest request) {
        String requestId = request.getRequestId() != null ? request.getRequestId() : UUID.randomUUID().toString();
        
        log.info("URL {} için performans önerileri stream'i başlatılıyor (requestId: {})", request.getUrl(), requestId);
        
        AIRequest aiRequest = new AIRequest();
        aiRequest.setRequestId(requestId);
        aiRequest.setUserId(request.getUserId());
        aiRequest.setModel(request.getModel() != null ? request.getModel() : "google/gemini-2.0-flash-lite-001");
        aiRequest.setMaxTokens(request.getMaxTokens() != null ? request.getMaxTokens() : 2000);
        aiRequest.setTemperature(request.getTemperature() != null ? request.getTemperature() : 0.7);
        aiRequest.setLanguage(request.getLanguage() != null ? request.getLanguage() : "tr"); // Varsayılan olarak Türkçe yanıt
        
        // Performans önerileri için system prompt
        String systemPrompt = "Sen bir web performans optimizasyonu uzmanısın. " +
                "Verilen URL'yi analiz et ve spesifik iyileştirmeler öner. " +
                "Her sorun için şunları belirt: problem açıklaması, önem derecesi (kritik/önemli/düşük), çözüm, " +
                "kod örneği, faydalı kaynaklar ve uygulama zorluğu (kolay/orta/zor). " +
                "Yanıtını geçerli bir JSON dizisi olarak formatla.";
                
        aiRequest.setSystemPrompt(systemPrompt);
        
        // URL'le prompt oluştur
        String prompt = "Bu URL için performans ve kullanıcı deneyimi sorunlarını analiz et: " + request.getUrl() + 
                "\n\nHer bir sorun için şu bilgileri içeren bir JSON dizisi oluştur:\n" +
                "1. problem: Sorunun açıklaması\n" +
                "2. severity: Önem derecesi (critical, major, minor)\n" +
                "3. solution: Çözüm önerisi\n" +
                "4. codeExample: Kod örneği\n" +
                "5. resources: Faydalı kaynakların URL'lerini içeren bir dizi\n" +
                "6. implementationDifficulty: Uygulama zorluğu (easy, medium, hard)";
        
        aiRequest.setPrompt(prompt);
        
        return llmService.streamChatCompletion(aiRequest)
                .map(chunk -> {
                    StreamSuggestionsResponse response = new StreamSuggestionsResponse();
                    response.setRequestId(requestId);
                    response.setContent(chunk.getContent());
                    response.setDone(chunk.isDone());
                    response.setError(chunk.isError());
                    response.setPing(chunk.isPing());
                    return response;
                })
                .doOnError(error -> log.error("Stream suggestions error: {}", error.getMessage(), error));
    }
    
    /**
     * URL için performans analiz geçmişini getirir
     */
    public Mono<PerformanceHistoryResponse> getPerformanceHistory(PerformanceHistoryRequest request) {
        log.info("Getting performance history for URL: {}", request.getUrl());
        
        return performanceAnalysisRepository.findByUrl(request.getUrl())
                .map(analysis -> {
                    return PerformanceHistoryEntry.builder()
                        .id(analysis.getId())
                        .url(analysis.getUrl())
                        .timestamp(analysis.getTimestamp())
                        .performance(analysis.getPerformance())
                        .build();
                })
                .collectList()
                .map(entries -> {
                    return PerformanceHistoryResponse.builder()
                        .history(entries)
                        .build();
                });
    }
}
