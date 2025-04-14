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
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PerformanceService {
    private final LighthouseService lighthouseService;
    private final PerformanceAnalysisRepository performanceAnalysisRepository;
    private final PerformanceAnalysisCache performanceAnalysisCache;
    // AiService referansını kaldırdık, artık sadece LLMService kullanıyoruz
    private final PromptService promptService;
    private final MeterRegistry meterRegistry;
    private final LLMService llmService;
    private final ObjectMapper objectMapper;

    public Mono<PerformanceAnalysisResponse> analyzeWebsite(PerformanceAnalysisRequest request) {
        // URL'i önbellekte ara
        return performanceAnalysisCache.getAnalysisResult(request.getUrl())
                .switchIfEmpty(
                        // Önbellekte yoksa yeni analiz yap
                        lighthouseService.analyzeSite(request.getUrl())
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
        
        // URL ile prompt oluştur
        String prompt = "Bu web sitesini analiz et: " + request.getUrl() + "\n\n" +
                "Web sitesi performansını iyileştirmek için detaylı öneriler sun. Önce en etkili değişikliklere odaklan. " +
                "Yanıtını her biri için şu özelliklere sahip bir JSON dizisi olarak formatla: " +
                "problem, severity, solution, codeExample, resources (URL'ler dizisi), implementationDifficulty";
        
        aiRequest.setPrompt(prompt);
        
        // LLMService'in streaming özelliğini kullan
        return llmService.streamChatCompletion(aiRequest)
                .map(streamResponse -> {
                    if (streamResponse.isPing()) {
                        return StreamSuggestionsResponse.ping();
                    } else if (streamResponse.isError()) {
                        log.warn("Stream yanıtında hata oluştu: {}", streamResponse.getContent());
                        return StreamSuggestionsResponse.error(streamResponse.getContent());
                    } else {
                        return StreamSuggestionsResponse.content(
                                streamResponse.getContent(),
                                streamResponse.isDone()
                        );
                    }
                })
                .doOnComplete(() -> log.info("URL {} için performans önerileri stream'i tamamlandı", request.getUrl()))
                .doOnError(error -> log.error("Stream sırasında hata: {}", error.getMessage(), error));
    }
    
    public Mono<PerformanceHistoryResponse> getPerformanceHistory(PerformanceHistoryRequest request) {
        return performanceAnalysisRepository.findByUrl(request.getUrl())
                .collectList()
                .map(analysisList -> {
                    List<PerformanceHistoryResponse.PerformanceHistoryEntry> historyEntries = analysisList.stream()
                            .map(analysis -> PerformanceHistoryResponse.PerformanceHistoryEntry.builder()
                                    .id(analysis.getId())
                                    .url(analysis.getUrl())
                                    .timestamp(analysis.getTimestamp())
                                    .performance(analysis.getPerformance())
                                    .build())
                            .collect(Collectors.toList());
                    
                    return PerformanceHistoryResponse.builder()
                            .history(historyEntries)
                            .build();
                });
    }
}
