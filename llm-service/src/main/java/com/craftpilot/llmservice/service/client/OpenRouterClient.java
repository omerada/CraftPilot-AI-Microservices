package com.craftpilot.llmservice.service.client;

import com.craftpilot.llmservice.config.OpenRouterProperties;
import com.craftpilot.llmservice.exception.APIException;
import com.craftpilot.llmservice.model.AIRequest;
import com.craftpilot.llmservice.model.AIResponse;
import com.craftpilot.llmservice.util.LoggingUtils;
import com.craftpilot.llmservice.util.RequestBodyBuilder;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
// reactor.util.retry.Retry'ı tam adıyla kullanacağız, import etmiyoruz
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * OpenRouter API ile iletişim sağlayan istemci sınıfı
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OpenRouterClient {
    private final WebClient openRouterWebClient;
    private final RequestBodyBuilder requestBodyBuilder;
    private final OpenRouterProperties properties;
    
    private static final String OPENROUTER_CIRCUIT = "openRouterApiCircuit";
    private static final String FALLBACK_RESPONSE = "API servisine şu anda ulaşılamıyor. Lütfen daha sonra tekrar deneyin.";

    /**
     * OpenRouter API'ye JSON formatında istek gönderir ve yanıtı alır
     */
    @CircuitBreaker(name = OPENROUTER_CIRCUIT, fallbackMethod = "callOpenRouterFallback")
    @Retry(name = OPENROUTER_CIRCUIT)
    public Mono<Map<String, Object>> callOpenRouter(String endpoint, AIRequest request) {
        Map<String, Object> requestBody = requestBodyBuilder.createRequestBody(request);
        
        // Endpoint normalizasyonu
        String uri = normalizeEndpoint(endpoint);
        
        log.debug("OpenRouter isteği: {} - Body: {}", uri, LoggingUtils.truncateForLogging(requestBody.toString(), 200));
        
        return openRouterWebClient.post()
            .uri(uri)
            .bodyValue(requestBody)
            .headers(headers -> {
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("Accept", "application/json, text/plain, text/html, */*");
            })
            .exchangeToMono(this::processResponse)
            .timeout(Duration.ofSeconds(properties.getRequestTimeoutSeconds()))
            .doOnError(e -> log.error("OpenRouter API isteği sırasında hata: {}", e.getMessage(), e))
            // Timeout ve ağ hataları için retry mekanizması
            .retryWhen(reactor.util.retry.Retry.backoff(2, Duration.ofMillis(300))
                .filter(throwable -> throwable instanceof TimeoutException || 
                                    throwable.getMessage().contains("Connection refused"))
                .doBeforeRetry(retrySignal -> 
                    log.warn("OpenRouter API isteği yeniden deneniyor: {}, Hata: {}", 
                        uri, retrySignal.failure().getMessage())))
            // Ağır işlemleri async thread'e taşı
            .subscribeOn(Schedulers.boundedElastic());
    }
    
    /**
     * OpenRouter API'ye stream formatında istek gönderir ve yanıtı alır
     */
    @CircuitBreaker(name = OPENROUTER_CIRCUIT, fallbackMethod = "streamFromOpenRouterFallback")
    public Flux<String> streamFromOpenRouter(AIRequest request) {
        Map<String, Object> requestBody = requestBodyBuilder.createRequestBody(request);
        requestBody.put("stream", true);
        
        log.debug("OpenRouter stream isteği gönderiliyor: {}", LoggingUtils.truncateForLogging(requestBody.toString(), 200));
        
        return openRouterWebClient.post()
            .uri("/chat/completions")
            .bodyValue(requestBody)
            .headers(headers -> {
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("Accept", "text/event-stream");
            })
            .retrieve()
            .onStatus(status -> status.is5xxServerError(), response -> 
                response.bodyToMono(String.class)
                    .flatMap(error -> Mono.error(new APIException("OpenRouter sunucu hatası: " + error))))
            .bodyToFlux(String.class)
            .timeout(Duration.ofSeconds(properties.getStreamTimeoutSeconds()))
            .doOnSubscribe(s -> log.debug("OpenRouter stream'e abone olundu"))
            .doOnComplete(() -> log.info("OpenRouter stream başarıyla tamamlandı"))
            .doOnError(e -> log.error("OpenRouter stream hatası: {}", e.getMessage(), e))
            // Backpressure stratejisi
            .onBackpressureBuffer(10000, bufferOverflowException -> 
                log.warn("Stream backpressure buffer aşıldı"))
            // Yavaş consumer'ları yönet
            .publishOn(Schedulers.boundedElastic(), 32);
    }
    
    /**
     * API bağlantı durumunu kontrol eder
     */
    @CircuitBreaker(name = OPENROUTER_CIRCUIT, fallbackMethod = "checkApiConnectionFallback")
    public Mono<Boolean> checkApiConnection() {
        return openRouterWebClient.get()
            .uri("/models")
            .retrieve()
            .bodyToMono(String.class)
            .map(response -> true)
            .onErrorResume(e -> {
                log.error("API bağlantı kontrolü başarısız: {}", e.getMessage());
                return Mono.just(false);
            })
            .timeout(Duration.ofSeconds(5), Mono.just(false))
            .cache(Duration.ofMinutes(1)); // 1 dakika boyunca önbelleğe al, sürekli istek atma
    }
    
    /**
     * Endpoint yolunu normalleştirir
     */
    private String normalizeEndpoint(String endpoint) {
        if (endpoint.startsWith("/")) {
            endpoint = endpoint.substring(1);
        }
        
        if (endpoint.startsWith("api/v1") || endpoint.startsWith("api/v1/")) {
            return "/" + endpoint;
        } else {
            return "/api/v1/" + endpoint;
        }
    }
    
    /**
     * HTTP yanıtını işler ve uygun formatta döndürür
     */
    private Mono<Map<String, Object>> processResponse(ClientResponse response) {
        if (response.statusCode().is2xxSuccessful()) {
            MediaType contentType = response.headers().contentType().orElse(MediaType.APPLICATION_JSON);
            
            if (contentType.includes(MediaType.APPLICATION_JSON)) {
                return response.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
            } else if (contentType.includes(MediaType.TEXT_HTML) || contentType.includes(MediaType.TEXT_PLAIN)) {
                return response.bodyToMono(String.class)
                        .flatMap(htmlContent -> {
                            log.error("HTML yanıtı alındı: {} karakterlik içerik", 
                                htmlContent != null ? htmlContent.length() : 0);
                            Map<String, Object> errorMap = new HashMap<>();
                            errorMap.put("error", "API HTML yanıtı döndü. Servis geçici olarak kullanılamıyor olabilir.");
                            return Mono.just(errorMap);
                        });
            } else {
                log.warn("Beklenmeyen içerik türü: {}", contentType);
                Map<String, Object> errorMap = new HashMap<>();
                errorMap.put("error", "Beklenmeyen içerik türü: " + contentType);
                return Mono.just(errorMap);
            }
        } else {
            return response.bodyToMono(String.class)
                .flatMap(error -> {
                    String errorMessage = "API hatası: " + response.statusCode() + 
                        " - Yanıt: " + (error != null ? error : "Boş yanıt");
                    log.error(errorMessage);
                    Map<String, Object> errorMap = new HashMap<>();
                    errorMap.put("error", errorMessage);
                    return Mono.just(errorMap);
                })
                .onErrorResume(e -> {
                    log.error("API yanıtı okunurken hata: {}", e.getMessage());
                    Map<String, Object> errorMap = new HashMap<>();
                    errorMap.put("error", "API yanıtı işlenirken hata: " + e.getMessage());
                    return Mono.just(errorMap);
                });
        }
    }
    
    /**
     * callOpenRouter metodu için fallback
     */
    private Mono<Map<String, Object>> callOpenRouterFallback(String endpoint, AIRequest request, Exception ex) {
        log.warn("callOpenRouter için fallback tetiklendi, endpoint: {}, hata: {}", endpoint, ex.getMessage());
        Map<String, Object> fallbackResponse = new HashMap<>();
        fallbackResponse.put("error", FALLBACK_RESPONSE);
        return Mono.just(fallbackResponse);
    }
    
    /**
     * streamFromOpenRouter metodu için fallback
     */
    private Flux<String> streamFromOpenRouterFallback(AIRequest request, Exception ex) {
        log.warn("streamFromOpenRouter için fallback tetiklendi, hata: {}", ex.getMessage());
        return Flux.just("data: " + FALLBACK_RESPONSE, "data: [DONE]");
    }
    
    /**
     * checkApiConnection metodu için fallback
     */
    private Mono<Boolean> checkApiConnectionFallback(Exception ex) {
        log.warn("checkApiConnection için fallback tetiklendi, hata: {}", ex.getMessage());
        return Mono.just(false);
    }
    
    // Bu metodun içinde reactor.util.retry.Retry yerine tam adı kullanılmalı
    @Retry(name = "openRouter")
    public Mono<AIResponse> sendRequest(AIRequest request) {
        // Metot implementasyonu
        return Mono.empty();
    }
}
