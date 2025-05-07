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
    
    @Value("${extraction.retries:3}")
    private int maxRetries;
    
    @Value("${extraction.retry.backoff.ms:500}")
    private long retryBackoffMs;

    public Mono<ExtractedUserInfo> extractUserInfo(String userId, String message) {
        if (userId == null || message == null || message.trim().isEmpty()) {
            log.debug("Skipping extraction for empty/null message or userId");
            return Mono.empty();
        }
        
        // Bellek yönetimi iyileştirmesi - uzun mesajları kısalt
        final String effectiveMessage;
        if (message.length() > 4000) {
            StringBuilder sb = new StringBuilder(4100);
            sb.append(message.substring(0, 2000))
              .append("\n...[içerik kısaltıldı]...\n")
              .append(message.substring(message.length() - 2000));
            effectiveMessage = sb.toString();
            log.debug("Büyük mesaj kısaltıldı: {} -> {} karakter", message.length(), effectiveMessage.length());
        } else {
            effectiveMessage = message;
        }
        
        String systemPrompt = "Sen bir kullanıcı bilgisi çıkarma asistanısın. Kullanıcının mesajını analiz et ve " +
                "kişisel bilgilerini, tercihlerini, veya diğer önemli detayları belirle. " +
                "Gerçeklere dayalı, doğrulanabilir bilgilere odaklan. " +
                "Çıkardığın bilgileri JSON formatında döndür.";

        String prompt = buildExtractionPrompt(effectiveMessage);

        AIRequest request = AIRequest.builder()
                .model(extractionModel)
                .prompt(prompt)
                .systemPrompt(systemPrompt)
                .temperature(0.3)
                .maxTokens(256)
                .stream(false) // Bilgi çıkarımı için stream modu kapatıldı
                .build();

        LoggingUtils.setRequestContext(request.getRequestId(), userId);
        log.debug("Extraction request created for userId={}, messageLength={}", 
                userId, effectiveMessage.length());
        
        return llmService.processChatCompletion(request)
                .timeout(Duration.ofSeconds(extractionTimeoutSeconds))
                .publishOn(Schedulers.boundedElastic())
                .doOnSuccess(response -> log.info("Received AI response for extraction: length={}", 
                        response != null && response.getResponse() != null ? response.getResponse().length() : 0))
                .doOnError(e -> log.error("Error during LLM extraction request: {}", e.getMessage()))
                .onErrorResume(e -> {
                    log.error("Error while extracting user info: {}", e.getMessage());
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

    public Mono<Void> processAndStoreUserInfo(String userId, String message) {
        if (userId == null || message == null || message.trim().isEmpty()) {
            log.debug("Skipping extraction for empty/null message or userId");
            return Mono.empty();
        }
        
        LoggingUtils.setRequestContext(null, userId);
        log.info("Processing message for user {}: {}", userId, 
                LoggingUtils.truncateForLogging(message, 50));
        
        AtomicInteger retryCount = new AtomicInteger(0);
        
        return extractUserInfo(userId, message)
                .timeout(Duration.ofSeconds(extractionTimeoutSeconds + 5))
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
                    return Mono.empty();
                })
                .flatMap(extractedInfo -> {
                    if (extractedInfo == null || "NO_INFORMATION".equals(extractedInfo.getInformation()) 
                            || "EXTRACTION_ERROR".equals(extractedInfo.getInformation())) {
                        log.info("No useful information extracted for user {}, skipping memory storage", userId);
                        return Mono.empty();
                    }

                    extractedInfo.setUserId(userId);
                    extractedInfo.setTimestamp(Instant.now());
                    
                    extractedInfo.setContext("Mesajdan çıkarıldı: " + 
                        LoggingUtils.truncateForLogging(message, 30));

                    log.info("Çıkarılan bilgi: {}", extractedInfo.getInformation());
                    
                    return userMemoryClient.addMemoryEntry(extractedInfo)
                            .timeout(Duration.ofSeconds(memoryTimeoutSeconds))
                            .doOnSubscribe(s -> log.info("Calling user-memory-service for user {}", userId))
                            .doOnSuccess(result -> log.info("Successfully stored AI-extracted information for user {}", userId))
                            .doOnError(error -> {
                                log.error("Failed to store AI-extracted information for user {}: {} (Type: {})", 
                                        userId, error.getMessage(), error.getClass().getName());
                                if (error instanceof WebClientResponseException) {
                                    WebClientResponseException wcre = (WebClientResponseException) error;
                                    log.error("Response details: Status={}, Body={}", 
                                            wcre.getStatusCode(), wcre.getResponseBodyAsString());
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

    private String buildExtractionPrompt(String message) {
        return "Aşağıdaki mesajdan kullanıcı hakkında kişisel bilgileri çıkar. " +
               "Eğer kişisel bilgi bulunamazsa, 'NO_INFORMATION' şeklinde yanıt ver. " +
               "Şu bilgileri çıkarmaya odaklan: isim, yaş, meslek, konum, ilgi alanları, hobiler, tercihler, aile detayları. " +
               "JSON formatında yanıt ver, sadece 'information' alanı içinde çıkarılan bilgiyi veya 'NO_INFORMATION' değerini döndür. " +
               "Örnek yanıt: {\"information\": \"İsim: Ahmet. Yaş: 30. Konum: İstanbul. İlgi alanları: Teknoloji, yazılım.\"}" +
               "\n\nKullanıcı mesajı: " + message;
    }

    private Mono<ExtractedUserInfo> parseExtractionResponse(String userId, AIResponse response, String originalMessage) {
        try {
            if (response == null || response.getResponse() == null) {
                log.warn("Null response received from LLM service");
                return Mono.empty();
            }
            
            String content = response.getResponse();
            
            if (content.contains("NO_INFORMATION") || content.contains("EXTRACTION_ERROR")) {
                log.debug("Mesajdan bilgi çıkarılamadı");
                return Mono.empty();
            }

            // JSON ayrıştırma deneyin
            try {
                String jsonContent = content;
                
                // İçerikten JSON bölümünü çıkar
                int jsonStart = content.indexOf('{');
                int jsonEnd = content.lastIndexOf('}');
                
                if (jsonStart >= 0 && jsonEnd > jsonStart) {
                    jsonContent = content.substring(jsonStart, jsonEnd + 1);
                }
                
                JsonNode jsonNode = objectMapper.readTree(jsonContent);
                
                if (jsonNode.has("information")) {
                    String information = jsonNode.get("information").asText();
                    
                    if (information.isEmpty() || "NO_INFORMATION".equals(information)) {
                        return Mono.empty();
                    }
                    
                    log.info("Successfully parsed information from AI response: {}", information);
                    
                    return Mono.just(ExtractedUserInfo.builder()
                            .userId(userId)
                            .information(information)
                            .context("Mesajdan çıkarıldı: " + 
                                    LoggingUtils.truncateForLogging(originalMessage, 30))
                            .timestamp(Instant.now())
                            .build());
                } else {
                    log.warn("information field not found in JSON response");
                }
            } catch (Exception ex) {
                log.warn("Failed to parse JSON from AI response, falling back to string parsing: {}", ex.getMessage());
                // JSON parse hatası durumunda string parsing
            }
            
            // Alternatif manuel parsing (eski yöntem - yedek olarak)
            int start = content.indexOf("\"information\"");
            if (start == -1) {
                log.debug("AI yanıtında information alanı bulunamadı");
                return Mono.empty();
            }

            // Bilgi değerini çıkar
            start = content.indexOf(":", start) + 1;
            int end = content.indexOf("\"", content.indexOf("\"", start) + 1) + 1;
            String information = content.substring(start, end).trim();
            
            // Tırnak işaretlerini kaldır
            if (information.startsWith("\"") && information.endsWith("\"")) {
                information = information.substring(1, information.length() - 1);
            }

            if (information.isEmpty() || information.equals("NO_INFORMATION")) {
                return Mono.empty();
            }

            log.info("Successfully extracted information using string parsing: {}", information);
            
            return Mono.just(ExtractedUserInfo.builder()
                    .userId(userId)
                    .information(information)
                    .context("Mesajdan çıkarıldı: " + 
                             LoggingUtils.truncateForLogging(originalMessage, 30))
                    .timestamp(Instant.now())
                    .build());

        } catch (Exception e) {
            log.error("Çıkarım yanıtı ayrıştırılırken hata oluştu: {}", e.getMessage(), e);
            return Mono.empty();
        }
    }
}
