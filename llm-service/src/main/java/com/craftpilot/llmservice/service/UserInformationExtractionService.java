package com.craftpilot.llmservice.service;

import com.craftpilot.llmservice.client.UserMemoryClient;
import com.craftpilot.llmservice.dto.ExtractedUserInfo;
import com.craftpilot.llmservice.model.AIRequest;
import com.craftpilot.llmservice.model.AIResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserInformationExtractionService {
    private final UserMemoryClient userMemoryClient;
    private final LLMService llmService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ai.model.extraction:google/gemma-3-4b-it}")
    private String extractionModel;

    // Performans ve ölçeklenebilirlik için yapılandırma parametreleri ekleyelim
    @Value("${extraction.timeout.seconds:10}")
    private int extractionTimeoutSeconds;
    
    @Value("${extraction.memory.timeout.seconds:5}")
    private int memoryTimeoutSeconds;
    
    @Value("${extraction.retry.max:3}")
    private int maxRetryAttempts;
    
    @Value("${extraction.retry.backoff.ms:500}")
    private long retryBackoffMs;

    // Context parametresi kaldırıldı
    public Mono<ExtractedUserInfo> extractUserInfo(String userId, String message) {
        if (userId == null || message == null || message.trim().isEmpty()) {
            log.debug("Skipping extraction for empty/null message or userId");
            return Mono.empty();
        }
        
        // Bellek yönetimi iyileştirmesi - uzun mesajları hem başından hem sonundan alarak özetleme
        final String effectiveMessage;
        if (message.length() > 4000) {
            StringBuilder sb = new StringBuilder(4100); // Önceden belirlenmiş kapasite ile
            sb.append(message.substring(0, 2000))
              .append("\n...[içerik kısaltıldı]...\n")
              .append(message.substring(message.length() - 2000));
            effectiveMessage = sb.toString();
            log.debug("Büyük mesaj kısaltıldı: {} -> {} karakter", message.length(), effectiveMessage.length());
        } else {
            effectiveMessage = message;
        }
        
        // Optimize edilmiş sistem promptu - mesajdan bilgi çıkarma amacını vurgula
        String systemPrompt = "Sen bir kullanıcı bilgisi çıkarma asistanısın. Kullanıcının mesajını analiz et ve " +
                "kişisel bilgilerini, tercihlerini, veya diğer önemli detayları belirle. " +
                "Gerçeklere dayalı, doğrulanabilir bilgilere odaklan. " +
                "Çıkardığın bilgileri JSON formatında döndür.";

        // Sadece mesaj içeriğini kullan
        String prompt = "Aşağıdaki mesajdan kullanıcıya ait bilgileri analiz et ve JSON formatında döndür:\n\n" +
                "```\n" + effectiveMessage + "\n```\n\n" +
                "JSON formatında döndür { \"information\": \"çıkarılan bilgi\" }";

        AIRequest request = AIRequest.builder()
                .model(extractionModel)
                .prompt(prompt)
                .systemPrompt(systemPrompt)
                .temperature(0.3)
                .maxTokens(256)
                .build();

        log.debug("Sending extraction request to LLM service: {}", prompt.substring(0, Math.min(100, prompt.length())) + "...");
        
        return llmService.processChatCompletion(request)
                .timeout(Duration.ofSeconds(extractionTimeoutSeconds))  // Timeout ekle
                .publishOn(Schedulers.boundedElastic())  // Uzun işlemleri sınırlı bir thread havuzunda yap
                .doOnSuccess(response -> log.info("Received AI response for extraction: length={}", 
                        response != null && response.getResponse() != null ? response.getResponse().length() : 0))
                .doOnError(e -> log.error("Error during LLM extraction request: {}", e.getMessage()))
                .flatMap(response -> parseExtractionResponse(userId, response, message))
                .retryWhen(Retry.backoff(maxRetryAttempts, Duration.ofMillis(retryBackoffMs))  // Retry ekle
                        .filter(e -> !(e instanceof IllegalArgumentException))  // Geçici hataları filtrele
                        .doBeforeRetry(signal -> 
                            log.warn("Retrying extraction after error: {}, attempt: {}", 
                                    signal.failure().getMessage(), signal.totalRetries() + 1)));
    }

    public Mono<Void> processAndStoreUserInfo(String userId, String message) {
        if (userId == null || message == null || message.trim().isEmpty()) {
            log.debug("Skipping extraction for empty/null message or userId");
            return Mono.empty();
        }
        
        log.info("Processing message for user {}: {}", userId, message.substring(0, Math.min(50, message.length())));
        
        // AI tabanlı bilgi çıkarımını kullan
        return extractUserInfo(userId, message)
                .doOnSubscribe(s -> log.debug("Starting extraction process for user {}", userId))
                .flatMap(extractedInfo -> {
                    if (extractedInfo == null || extractedInfo.getInformation() == null || extractedInfo.getInformation().isEmpty()) {
                        log.debug("No information extracted from message using AI for user {}", userId);
                        return Mono.empty();
                    }
                    
                    log.info("AI extracted user information: {}", extractedInfo.getInformation());
                    
                    // Timestamp'i burada ayarlayalım (client'ta kaybolabilir)
                    extractedInfo.setTimestamp(Instant.now());
                    
                    // Context alanını doğrudan mesajdan oluştur
                    extractedInfo.setContext("Mesajdan çıkarıldı: " + 
                        (message.length() > 30 ? 
                            message.substring(0, 30) + "..." : 
                            message));
                    
                    // User Memory servisine bilgileri gönder
                    log.debug("Sending extracted information to user-memory-service: userId={}, info={}", 
                            userId, extractedInfo.getInformation());
                    
                    return userMemoryClient.addMemoryEntry(extractedInfo)
                            .timeout(Duration.ofSeconds(memoryTimeoutSeconds))  // Timeout ekle
                            .doOnSubscribe(s -> log.debug("Calling user-memory-service for user {}", userId))
                            .doOnSuccess(result -> log.info("Successfully stored AI-extracted information for user {}", userId))
                            .doOnError(error -> log.error("Failed to store AI-extracted information for user {}: {}", 
                                    userId, error.getMessage()))
                            .retryWhen(Retry.backoff(maxRetryAttempts, Duration.ofMillis(retryBackoffMs))  // Retry ekle
                                    .filter(e -> !(e instanceof IllegalArgumentException))
                                    .doBeforeRetry(signal -> 
                                        log.warn("Retrying memory storage after error: {}, attempt: {}", 
                                                signal.failure().getMessage(), signal.totalRetries() + 1)))
                            .doFinally(signal -> log.debug("Memory storage completed with signal: {}", signal));
                })
                .then()
                .doOnSuccess(v -> log.info("Completed entire extraction and storage process for user {}", userId))
                .doOnError(e -> log.error("Error in extraction and storage process: {}", e.getMessage()));
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
            log.debug("Parsing AI response: {}", content);
            
            if (content.contains("NO_INFORMATION")) {
                log.debug("Mesajdan bilgi çıkarılamadı");
                return Mono.empty();
            }

            // Jackson ObjectMapper ile JSON ayrıştırma deneyin
            try {
                // İç içe JSON veya yalnızca JSON parçasını işleme
                // Yanıt bazen JSON olmayabilir veya JSON'ı bir metin içinde barındırabilir
                String jsonContent = content;
                
                // Başındaki ve sonundaki gereksiz metinleri temizle
                int jsonStart = content.indexOf("{");
                int jsonEnd = content.lastIndexOf("}") + 1;
                
                if (jsonStart >= 0 && jsonEnd > jsonStart) {
                    jsonContent = content.substring(jsonStart, jsonEnd);
                }
                
                JsonNode rootNode = objectMapper.readTree(jsonContent);
                JsonNode infoNode = rootNode.get("information");
                
                if (infoNode != null && !infoNode.isNull()) {
                    String information = infoNode.asText();
                    
                    if (information.isEmpty() || "NO_INFORMATION".equals(information)) {
                        log.debug("No information found in parsed JSON");
                        return Mono.empty();
                    }
                    
                    log.info("Successfully parsed information from AI response: {}", information);
                    
                    return Mono.just(ExtractedUserInfo.builder()
                            .userId(userId)
                            .information(information)
                            .context("Mesajdan çıkarıldı: " + 
                                    (originalMessage.length() > 30 ? 
                                        originalMessage.substring(0, 30) + "..." : 
                                        originalMessage))
                            .timestamp(Instant.now())
                            .build());
                } else {
                    log.warn("information field not found in JSON response");
                }
            } catch (Exception ex) {
                log.warn("Failed to parse JSON from AI response, falling back to string parsing: {}", ex.getMessage());
                // Hata durumunda eski string parsing yöntemine düş
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
                             (originalMessage.length() > 30 ? 
                                 originalMessage.substring(0, 30) + "..." : 
                                 originalMessage))
                    .timestamp(Instant.now())
                    .build());

        } catch (Exception e) {
            log.error("Çıkarım yanıtı ayrıştırılırken hata oluştu: {}", e.getMessage(), e);
            return Mono.empty();
        }
    }

}
