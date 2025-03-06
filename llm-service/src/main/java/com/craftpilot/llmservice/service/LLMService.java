package com.craftpilot.llmservice.service;

import com.craftpilot.llmservice.model.AIRequest;
import com.craftpilot.llmservice.model.AIResponse;
import com.craftpilot.llmservice.model.StreamResponse;  // Yeni import
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.Duration;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.ClientResponse;
import com.craftpilot.llmservice.exception.APIException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class LLMService {
    private final WebClient openRouterWebClient; 
    private final ObjectMapper objectMapper;

    public Mono<AIResponse> processChatCompletion(AIRequest request) {
        return callOpenRouter("chat/completions", request)  // URL düzeltildi
            .doOnNext(response -> log.debug("OpenRouter yanıtı: {}", response))
            .map(response -> {
                try {
                    return mapToAIResponse(response, request);
                } catch (Exception e) {
                    log.error("AI yanıtı işlenirken hata: {}", e.getMessage(), e);
                    throw new RuntimeException("AI yanıtı haritalanırken hata: " + e.getMessage(), e);
                }
            })
            .timeout(Duration.ofSeconds(60))
            .doOnError(e -> log.error("Chat completion error: {}", e.getMessage(), e))
            .onErrorResume(e -> {
                log.error("Hata yakalandı ve işlendi: {}", e.getMessage());
                return Mono.just(AIResponse.error("AI servisi hatası: " + e.getMessage()));
            });
    }

    public Mono<AIResponse> processCodeCompletion(AIRequest request) {
        // Code completion için özel model seçimi
        // request.setModel("anthropic/claude-2");
        return callOpenRouter("/v1/chat/completions", request)
            .map(response -> mapToAIResponse(response, request));
    }

    // OpenRouter görsel modeli desteklemediği için alternatif servis kullanımı
    public Mono<AIResponse> processImageGeneration(AIRequest request) {
        return Mono.error(new UnsupportedOperationException(
            "Image generation is not supported by OpenRouter. Please use Stability AI or DALL-E API directly."));
    }

    public Flux<StreamResponse> streamChatCompletion(AIRequest request) {
        Map<String, Object> requestBody = createRequestBody(request);
        requestBody.put("stream", true);
        
        log.debug("Stream isteği gönderiliyor: {}", requestBody);
        
        return openRouterWebClient.post()
            .uri("/chat/completions")  // URL düzeltildi
            .bodyValue(requestBody)
            .headers(headers -> {
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("Accept", "text/event-stream");
            })
            .retrieve()
            .bodyToFlux(String.class)
            .doOnSubscribe(s -> log.debug("Stream başlatılıyor"))
            .doOnNext(chunk -> log.debug("Ham chunk alındı: {}", chunk))
            .filter(chunk -> chunk != null && !chunk.trim().isEmpty())
            .map(chunk -> {
                if (chunk.startsWith("data: ")) {
                    chunk = chunk.substring(6).trim();
                }
                if (chunk.equals("[DONE]")) {
                    return StreamResponse.builder()
                        .content("")
                        .done(true)
                        .build();
                }
                
                try {
                    log.debug("JSON parse ediliyor: {}", chunk);
                    Map<String, Object> response = objectMapper.readValue(chunk, new TypeReference<Map<String, Object>>() {});
                    
                    if (response.containsKey("choices")) {
                        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                        if (!choices.isEmpty()) {
                            Map<String, Object> choice = choices.get(0);
                            Map<String, Object> delta = (Map<String, Object>) choice.get("delta");
                            
                            String content = "";
                            if (delta != null && delta.containsKey("content")) {
                                content = (String) delta.get("content");
                            }
                            
                            boolean isDone = choice.containsKey("finish_reason") && 
                                           choice.get("finish_reason") != null;
                            
                            return StreamResponse.builder()
                                .content(content)
                                .done(isDone)
                                .build();
                        }
                    }
                    
                    log.warn("Beklenmeyen yanıt formatı: {}", response);
                    return null;
                    
                } catch (Exception e) {
                    log.error("Chunk işlenirken hata: {} - Chunk: {}", e.getMessage(), chunk, e);
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .doOnError(e -> log.error("Stream işleminde hata: ", e))
            .doOnComplete(() -> log.debug("Stream tamamlandı"));
    }

    private Mono<Map<String, Object>> callOpenRouter(String endpoint, AIRequest request) {
        Map<String, Object> requestBody = createRequestBody(request);
        log.debug("OpenRouter isteği: {} - Body: {}", endpoint, requestBody);
        
        return openRouterWebClient.post()
            .uri(endpoint)  // /v1 kaldırıldı
            .bodyValue(requestBody)
            .headers(headers -> {
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
            })
            .retrieve()
            .onStatus(HttpStatusCode::isError, this::handleError)
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
            .timeout(Duration.ofSeconds(30))
            .doOnError(e -> log.error("OpenRouter API error: ", e));
    }

    private Mono<? extends Throwable> handleError(ClientResponse response) {
        return response.bodyToMono(String.class)
            .flatMap(error -> {
                String message = String.format("OpenRouter API Error [%s]: %s", 
                    response.statusCode(), error);
                log.error(message);
                return Mono.error(new APIException(message));
            });
    }

    private Map<String, Object> createRequestBody(AIRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", request.getModel());
        
        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", request.getPrompt());  // Direkt String olarak gönder
        messages.add(userMessage);
        
        body.put("messages", messages);
        body.put("max_tokens", request.getMaxTokens());
        body.put("temperature", request.getTemperature());
        
        log.debug("Oluşturulan request body: {}", body);
        return body;
    }

    private AIResponse mapToAIResponse(Map<String, Object> openRouterResponse, AIRequest request) {
        log.debug("OpenRouter yanıtı haritalanıyor: {}", openRouterResponse);
        
        String responseText = extractResponseText(openRouterResponse);
        if (responseText == null || responseText.trim().isEmpty()) {
            log.warn("OpenRouter'dan boş yanıt alındı");
            throw new RuntimeException("AI servisinden boş yanıt alındı");
        }

        AIResponse response = AIResponse.success(
            responseText,
            request.getModel(),
            extractTokenCount(openRouterResponse),
            request.getRequestId()
        );

        log.debug("Haritalanan yanıt: {}", response);
        return response;
    }

    private String extractResponseText(Map<String, Object> response) {
        try {
            log.debug("Yanıt içeriği: {}", response);
            
            // Daha ayrıntılı debugging ekleyelim
            log.debug("Response keys: {}", response.keySet());
            
            if (response.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                log.debug("Choices size: {}", choices.size());
                
                if (!choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    log.debug("Choice keys: {}", choice.keySet());
                    
                    // Choice içinde message veya text olabilir
                    if (choice.containsKey("message")) {
                        Map<String, Object> message = (Map<String, Object>) choice.get("message");
                        log.debug("Message keys: {}", message.keySet());
                        
                        if (message.containsKey("content")) {
                            Object content = message.get("content");
                            log.debug("Content type: {}", content != null ? content.getClass().getName() : "null");
                            
                            if (content instanceof String) {
                                return (String) content;
                            } else if (content instanceof List) {
                                List<Map<String, Object>> contents = (List<Map<String, Object>>) content;
                                return contents.stream()
                                    .filter(item -> "text".equals(item.get("type")))
                                    .map(item -> (String) item.get("text"))
                                    .findFirst()
                                    .orElse("");
                            }
                        }
                    }
                    // GPT türü modeller için text alanını kontrol et
                    else if (choice.containsKey("text")) {
                        return (String) choice.get("text");
                    }
                    // Ayrıca delta içinde de olabilir (özellikle stream yanıtlarında)
                    else if (choice.containsKey("delta")) {
                        Map<String, Object> delta = (Map<String, Object>) choice.get("delta");
                        if (delta.containsKey("content")) {
                            return (String) delta.get("content");
                        }
                    }
                }
            }
            
            // Farklı yanıt formatlarını işle
            if (response.containsKey("output") && response.get("output") instanceof String) {
                return (String) response.get("output");
            }
            
            if (response.containsKey("completion") && response.get("completion") instanceof String) {
                return (String) response.get("completion");
            }
            
            if (response.containsKey("generated_text") && response.get("generated_text") instanceof String) {
                return (String) response.get("generated_text");
            }
            
            // Eğer response'un kendisi String ise direkt döndür (bazı LLM API'leri için)
            if (response.size() == 1 && response.values().iterator().next() instanceof String) {
                return (String) response.values().iterator().next();
            }
            
            // Yanıt formatını JSON olarak logla
            try {
                log.error("Bilinmeyen yanıt formatı: {}", new ObjectMapper().writeValueAsString(response));
            } catch (Exception e) {
                log.error("Yanıt JSON dönüştürme hatası", e);
            }
            
            throw new RuntimeException("Geçersiz API yanıt formatı: " + response.keySet());
        } catch (Exception e) {
            log.error("Yanıt işlenirken hata oluştu: {}", e.getMessage(), e);
            throw new RuntimeException("AI yanıtı işlenemedi: " + e.getMessage(), e);
        }
    }

    private Integer extractTokenCount(Map<String, Object> response) {
        Map<String, Object> usage = (Map<String, Object>) response.getOrDefault("usage", Map.of());
        return ((Number) usage.getOrDefault("total_tokens", 0)).intValue();
    }
}