package com.craftpilot.llmservice.service;

import com.craftpilot.llmservice.client.UserMemoryClient;
import com.craftpilot.llmservice.dto.ExtractedUserInfo;
import com.craftpilot.llmservice.model.AIRequest;
import com.craftpilot.llmservice.model.AIResponse;
import com.craftpilot.llmservice.util.LoggingUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
// Import eksiklikleri için gereken sınıflar
import org.json.JSONObject;
import java.util.Iterator;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserInformationExtractionService {
    private final UserMemoryClient userMemoryClient;
    private final LLMService llmService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ai.model.extraction:google/gemma-3-4b-it}")
    private String extractionModel;

    @Value("${extraction.timeout.seconds:30}")
    private int extractionTimeoutSeconds;
    
    @Value("${memory.timeout.seconds:10}")
    private int memoryTimeoutSeconds;
    
    @Value("${extraction.retry.max:3}")
    private int maxRetries;
    
    @Value("${extraction.retry.backoff.ms:500}")
    private long retryBackoffMs;

    public Mono<ExtractedUserInfo> extractUserInfo(String userId, String message) {
        if (userId == null || message == null || message.trim().isEmpty()) {
            log.debug("Skipping extraction for empty/null message or userId");
            return Mono.empty();
        }

        String prompt = buildExtractionPrompt(message);
        log.debug("Extraction request created for userId={}, messageLength={}", userId, message.length());

        AIRequest extractionRequest = AIRequest.builder()
                .model(extractionModel)
                .prompt(prompt)
                .maxTokens(500)
                .temperature(0.2)
                .build();

        return llmService.processChatCompletion(extractionRequest)
                .timeout(Duration.ofSeconds(extractionTimeoutSeconds))
                .doOnSubscribe(s -> log.debug("Sending extraction request to AI for user {}", userId))
                .doOnSuccess(response -> {
                    String content = response != null && response.getResponse() != null 
                                    ? response.getResponse() : "";
                    log.info("Received AI response for extraction: length={}, content snippet: {}", 
                            content.length(), 
                            content.length() > 0 ? content.substring(0, Math.min(50, content.length())) : "EMPTY");
                })
                .onErrorResume(e -> {
                    log.error("Error while extracting user info: {} (Type: {})", e.getMessage(), e.getClass().getName());
                    return Mono.just(AIResponse.builder()
                            .response("{\"information\": \"EXTRACTION_ERROR\"}")
                            .build());
                })
                .flatMap(response -> parseExtractionResponse(userId, response, message))
                .retryWhen(Retry.backoff(maxRetries, Duration.ofMillis(retryBackoffMs))
                        .filter(e -> !(e instanceof IllegalArgumentException))
                        .doBeforeRetry(signal -> 
                            log.warn("Retrying extraction after error: {}, attempt: {}", 
                                    signal.failure().getMessage(), signal.totalRetries() + 1)))
                .doFinally(s -> LoggingUtils.clearRequestContext());
    }

    // Yeni eklenen metod: Boş ya da null AI yanıtı için fallback
    private Mono<AIResponse> handleEmptyResponse(AIResponse response) {
        if (response == null || response.getResponse() == null || response.getResponse().isEmpty()) {
            log.warn("Empty AI response received, using fallback response");
            return Mono.just(AIResponse.builder()
                    .response("{\"information\": \"NO_EXTRACTION_POSSIBLE\"}")
                    .build());
        }
        return Mono.just(response);
    }

    private Mono<ExtractedUserInfo> parseExtractionResponse(String userId, AIResponse response, String originalMessage) {
        if (response == null || response.getResponse() == null || response.getResponse().isEmpty()) {
            log.warn("AI response is null or empty for user {}", userId);
            ExtractedUserInfo fallbackInfo = new ExtractedUserInfo();
            fallbackInfo.setUserId(userId);
            fallbackInfo.setInformation("Kullanıcıdan bilgi çıkarılamadı");
            fallbackInfo.setSource("Fallback extraction");
            fallbackInfo.setContext("Boş AI yanıtı");
            fallbackInfo.setTimestamp(Instant.now());
            return Mono.just(fallbackInfo);
        }

        try {
            String jsonResponse = response.getResponse().trim();
            // JSON yanıtı düzeltme girişimi
            if (!jsonResponse.startsWith("{")) {
                log.warn("Non-JSON response received: {}", jsonResponse.substring(0, Math.min(50, jsonResponse.length())));
                jsonResponse = extractJsonFromText(jsonResponse);
            }
            
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode infoNode = root.get("information");
            
            if (infoNode == null || infoNode.isNull() || infoNode.asText().isEmpty()) {
                log.warn("No information field found in JSON response for user {}", userId);
                ExtractedUserInfo emptyInfo = new ExtractedUserInfo();
                emptyInfo.setUserId(userId);
                emptyInfo.setInformation("NO_INFORMATION");
                emptyInfo.setSource("AI extraction");
                emptyInfo.setContext("Mesaj analizi");
                emptyInfo.setTimestamp(Instant.now());
                return Mono.just(emptyInfo);
            }
            
            String extractedInfo = infoNode.asText();
            
            // Anlamsız ya da işlenemez yanıt kontrolü
            if (extractedInfo.equals("EXTRACTION_ERROR") || extractedInfo.equals("NO_INFORMATION")) {
                log.warn("AI returned non-useful extraction result: {}", extractedInfo);
                ExtractedUserInfo basicInfo = new ExtractedUserInfo();
                basicInfo.setUserId(userId);
                basicInfo.setInformation(extractedInfo);
                basicInfo.setSource("AI extraction - limited result");
                basicInfo.setContext("Mesaj: " + shortenMessage(originalMessage));
                basicInfo.setTimestamp(Instant.now());
                return Mono.just(basicInfo);
            }
            
            ExtractedUserInfo info = new ExtractedUserInfo();
            info.setUserId(userId);
            info.setInformation(extractedInfo);
            info.setSource("AI extraction");
            info.setContext("Mesaj analizi");
            info.setTimestamp(Instant.now());
            
            log.debug("Successfully parsed extraction response for user {}: {}", 
                      userId, shortenMessage(extractedInfo));
            return Mono.just(info);
            
        } catch (Exception e) {
            log.error("Error parsing extraction response: {}", e.getMessage());
            ExtractedUserInfo errorInfo = new ExtractedUserInfo();
            errorInfo.setUserId(userId);
            errorInfo.setInformation("PARSING_ERROR");
            errorInfo.setSource("Failed extraction");
            errorInfo.setContext("Hata: " + e.getMessage());
            errorInfo.setTimestamp(Instant.now());
            return Mono.just(errorInfo);
        }
    }

    // Yeni eklenen yardımcı metod
    private String extractJsonFromText(String text) {
        int jsonStart = text.indexOf('{');
        int jsonEnd = text.lastIndexOf('}');
        
        if (jsonStart >= 0 && jsonEnd > jsonStart) {
            return text.substring(jsonStart, jsonEnd + 1);
        }
        
        return "{\"information\": \"INVALID_RESPONSE_FORMAT\"}";
    }
    
    // Yeni eklenen yardımcı metod
    private String shortenMessage(String message) {
        if (message == null) return "";
        int maxLength = 30;
        if (message.length() <= maxLength) return message;
        return message.substring(0, maxLength) + "...";
    }

    public Mono<Void> processAndStoreUserInfo(String userId, String message) {
        // UserId kontrolü ekleyelim
        if (userId == null || userId.trim().isEmpty()) {
            log.warn("Cannot process user message with null or empty userId");
            return Mono.empty();
        }
        
        if (message == null || message.trim().isEmpty()) {
            log.warn("Cannot process empty message for user: {}", userId);
            return Mono.empty();
        }
        
        LoggingUtils.setRequestContext(null, userId);
        log.info("Processing message for user {}: {}", userId, 
                LoggingUtils.truncateForLogging(message, 50));
        
        AtomicInteger retryCount = new AtomicInteger(0);
        
        return extractUserInfo(userId, message)
                .timeout(Duration.ofSeconds(extractionTimeoutSeconds + 5))
                .defaultIfEmpty(createDefaultExtractedInfo(userId, message))
                .doOnSuccess(extractedInfo -> {
                    if (extractedInfo == null) {
                        log.warn("Extraction result is null for user {}", userId);
                    } else {
                        log.info("Information extracted successfully for user {}: {}", 
                            userId, LoggingUtils.truncateForLogging(extractedInfo.getInformation(), 50));
                    }
                })
                .onErrorResume(e -> {
                    int attempt = retryCount.getAndIncrement();
                    if (attempt < maxRetries) {
                        log.warn("Retrying extraction after error: {}, attempt: {}", e.getMessage(), attempt);
                        return Mono.delay(Duration.ofSeconds(1)).then(Mono.empty());
                    }
                    log.error("Failed to extract user info after {} attempts: {}", maxRetries, e.getMessage());
                    return Mono.just(createDefaultExtractedInfo(userId, message));
                })
                .flatMap(extractedInfo -> {
                    if (extractedInfo == null) {
                        log.warn("Extraction failed with null result for user {}", userId);
                        return Mono.empty(); // Bilgi yoksa işlemi sonlandır
                    }
                    
                    // Anlamlı bilgi var mı kontrol et
                    String info = extractedInfo.getInformation();
                    if (info == null || info.isEmpty() || 
                        "NO_INFORMATION".equals(info) || 
                        "EXTRACTION_ERROR".equals(info) ||
                        "PARSING_ERROR".equals(info) ||
                        "INVALID_RESPONSE_FORMAT".equals(info)) {
                        
                        // Anlamlı bilgi yoksa, belleğe kaydetme
                        log.info("No meaningful information extracted for user {}, skipping memory storage. Content: '{}'", 
                                userId, info != null ? info : "null");
                        return Mono.empty();
                    }

                    // Sadece anlamlı bilgi çıkarıldığında memory'ye kaydet
                    extractedInfo.setUserId(userId);
                    extractedInfo.setTimestamp(Instant.now());
                    
                    extractedInfo.setContext("Mesajdan çıkarıldı: " + 
                        LoggingUtils.truncateForLogging(message, 30));

                    log.info("Çıkarılan bilgi: {}", extractedInfo.getInformation());
                    
                    return userMemoryClient.addMemoryEntry(extractedInfo)
                            .timeout(Duration.ofSeconds(memoryTimeoutSeconds))
                            .doOnSubscribe(s -> log.info("Calling user-memory-service for user {}", userId))
                            .doOnSuccess(result -> log.info("Successfully stored AI-extracted information for user {}, response: {}", 
                                    userId, result))
                            .doOnError(error -> {
                                log.error("Failed to store AI-extracted information for user {}: {} (Type: {})", 
                                        userId, error.getMessage(), error.getClass().getName());
                                if (error instanceof WebClientResponseException) {
                                    WebClientResponseException wcre = (WebClientResponseException) error;
                                    log.error("Response details: Status={}, Body={}, Headers={}", 
                                            wcre.getStatusCode(), wcre.getResponseBodyAsString(), 
                                            wcre.getHeaders());
                                }
                            })
                            .onErrorResume(error -> {
                                log.error("Error resuming from memory storage failure: {}", error.getMessage());
                                return Mono.empty();
                            });
                })
                .retry(maxRetries)
                .then()
                .doOnSuccess(v -> log.info("Completed entire extraction and storage process for user {}", userId))
                .doOnError(e -> log.error("Error in extraction and storage process: {}", e.getMessage()))
                .doFinally(s -> LoggingUtils.clearRequestContext());
    }

    // Yeni eklenen yardımcı metod
    private ExtractedUserInfo createDefaultExtractedInfo(String userId, String message) {
        ExtractedUserInfo defaultInfo = new ExtractedUserInfo();
        defaultInfo.setUserId(userId);
        defaultInfo.setInformation("Kullanıcı mesaj gönderdi");
        defaultInfo.setSource("Varsayılan kayıt");
        defaultInfo.setContext("Mesaj: " + shortenMessage(message));
        defaultInfo.setTimestamp(Instant.now());
        return defaultInfo;
    }

    private String buildExtractionPrompt(String message) {
        return "Aşağıdaki kullanıcı mesajını analiz et ve kullanıcı hakkında bilgi çıkar. " +
               "Sadece belirgin, açık bilgileri al, tahmin yürütme. " +
               "İsim, yaş, konum, meslek, ilgi alanları, tercihler gibi bilgileri JSON formatında döndür.\n\n" +
               "Örnek yanıt format:\n{\"information\": \"[çıkarılan bilgi]\"}\n\n" +
               "Örneğin, 'Benim adım Ömer' için yanıt şu olmalıdır: {\"information\": \"Kullanıcının adı Ömer\"}\n\n" +
               "Eğer hiçbir bilgi bulamazsan, şunu döndür: {\"information\": \"NO_INFORMATION\"}\n\n" +
               "Mesaj: " + message;
    }

    public Mono<ExtractedUserInfo> extractUserInformation(String userId, String message, String context) {
        // UserId kontrolü ekleyelim
        if (userId == null || userId.trim().isEmpty()) {
            log.warn("Cannot extract information for null or empty userId");
            return Mono.error(new IllegalArgumentException("User ID cannot be null or empty"));
        }
        
        log.info("Processing message for user {}: {}", userId, message.length() > 50 ? 
                message.substring(0, 50) + "..." : message);
        
        // Mesaj çok kısa ise anlamlı bilgi çıkarmaya çalışmayalım
        if (message.trim().length() < 10) {
            log.info("Message too short for information extraction (length: {})", message.length());
            return Mono.just(ExtractedUserInfo.builder()
                    .userId(userId)
                    .information("Kullanıcı kısa bir mesaj gönderdi")
                    .source("Mesaj çok kısa")
                    .context(context)
                    .timestamp(Instant.now())
                    .build());
        }
        
        log.debug("Extraction request created for userId={}, messageLength={}", userId, message.length());
        
        return callExtractionService(userId, message)
                .timeout(Duration.ofSeconds(10))
                .doOnSuccess(response -> {
                    log.info("Received AI response for extraction: length={}, content snippet: {}", 
                        response != null ? response.length() : 0,
                        response != null && !response.isEmpty() ? 
                            (response.length() > 30 ? response.substring(0, 30) + "..." : response) : "EMPTY");
                })
                .onErrorResume(e -> {
                    log.error("Error extracting information with AI: {}", e.getMessage());
                    return Mono.just("Kullanıcı bir mesaj gönderdi");
                })
                .map(extractedInfo -> {
                    // Boş yanıt kontrolü
                    if (extractedInfo == null || extractedInfo.trim().isEmpty()) {
                        extractedInfo = "Kullanıcı mesaj gönderdi";
                    }
                    
                    log.info("Information extracted successfully for user {}: {}", userId, extractedInfo);
                    log.info("Çıkarılan bilgi: {}", extractedInfo);
                    
                    // Bilgiyi kullanıcı belleğine kaydet
                    return ExtractedUserInfo.builder()
                            .userId(userId)
                            .information(extractedInfo)
                            .source("AI analizi")
                            .context(context)
                            .timestamp(Instant.now())
                            .build();
                });
    }

    // ExtractionRequest sınıfını kullanmak yerine mevcut modelleri kullanalım
    private Mono<String> callExtractionService(String userId, String message) {
        if (userId == null || userId.isEmpty() || message == null || message.isEmpty()) {
            log.warn("Cannot extract information with null/empty userId or message");
            return Mono.empty();
        }
        
        // AIRequest veya başka bir uygun request modelini kullanın
        AIRequest request = new AIRequest();
        request.setUserId(userId);
        request.setPrompt(message);
        request.setRequestType("EXTRACTION");
        // Model varsayılan olarak Gemini kullanılabilir
        request.setModel("google/gemini-pro");
        
        // LLMService'deki doğru metodu çağıralım (processChatCompletion)
        return llmService.processChatCompletion(request)
            .map(response -> response.getResponse())
            .onErrorResume(e -> {
                log.error("Error while extracting information: {}", e.getMessage());
                return Mono.empty();
            });
    }
    
    private String createExtractionPrompt(String message) {
        return "Kullanıcının mesajından kişisel bilgileri çıkar ve JSON formatında dön. Eğer anlam çıkarılamıyorsa \"bilgi_yok\" döndür. " +
               "Kullanıcı mesajı: \"" + message + "\"\n\n" +
               "Örneğin: Eğer kullanıcı \"Benim adım Ali, İstanbul'da yaşıyorum ve 30 yaşındayım\" derse, " +
               "JSON olarak: {\"ad\": \"Ali\", \"konum\": \"İstanbul\", \"yaş\": 30}\n\n" +
               "Cevap sadece JSON formatında olmalı, ekstra açıklama yapma.";
    }
    
    private ExtractedUserInfo processAIResponse(String aiResponse, String userId) {
        if (aiResponse == null || aiResponse.isEmpty() || aiResponse.equals("EMPTY")) {
            log.warn("Empty or null AI response for user {}", userId);
            return new ExtractedUserInfo(
                userId,
                "Kullanıcı bilgisi çıkarılamadı",
                Instant.now(),
                "AI extraction",
                "Chat message analysis"
            );
        }
        
        // JSON formatı kontrolü ve işleme
        if (aiResponse.trim().startsWith("{") && aiResponse.trim().endsWith("}")) {
            try {
                // JSON'ı anlamlı metin haline çevirme
                JSONObject jsonObj = new JSONObject(aiResponse);
                StringBuilder info = new StringBuilder();
                
                if (jsonObj.has("bilgi_yok")) {
                    return new ExtractedUserInfo(
                        userId,
                        "Kullanıcı mesajından bilgi çıkarılamadı",
                        Instant.now(),
                        "AI extraction",
                        "Chat message analysis"
                    );
                }
                
                Iterator<String> keys = jsonObj.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    info.append(key).append(": ").append(jsonObj.get(key)).append(". ");
                }
                
                if (info.length() > 0) {
                    return new ExtractedUserInfo(
                        userId,
                        info.toString(),
                        Instant.now(),
                        "AI extraction",
                        "Chat message analysis"
                    );
                }
            } catch (Exception e) {
                log.error("Error parsing AI response JSON for user {}: {}", userId, e.getMessage());
            }
        }
        
        // Fallback: default format
        return new ExtractedUserInfo(
            userId,
            aiResponse,
            Instant.now(),
            "AI extraction",
            "Chat message analysis"
        );
    }
}
