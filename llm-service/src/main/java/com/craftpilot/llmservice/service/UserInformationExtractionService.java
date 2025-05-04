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

    // Bazı yaygın kişisel bilgi kalıpları
    private static final Pattern NAME_PATTERN = Pattern.compile("(benim (adım|ismim)|adım|ismim) (\\w+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern AGE_PATTERN = Pattern.compile("(yaşım|yaşındayım) (\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern LOCATION_PATTERN = Pattern.compile("(yaşıyorum|yaşamaktayım|şehrinde) (\\w+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PROFESSION_PATTERN = Pattern.compile("(mesleğim|işim|çalışıyorum) (\\w+)", Pattern.CASE_INSENSITIVE);

    public Mono<ExtractedUserInfo> extractUserInfo(String userId, String message, String context) {
        if (userId == null || message == null || message.trim().isEmpty()) {
            log.debug("Skipping extraction for empty/null message or userId");
            return Mono.empty();
        }
        
        log.info("Extracting info for user {} with AI model", userId);
        String prompt = buildExtractionPrompt(message);
        
        AIRequest request = AIRequest.builder()
                .model(extractionModel)
                .prompt(prompt)
                .temperature(0.1) // Daha deterministik sonuçlar için düşük sıcaklık
                .userId(userId) // UserId ekleyelim
                .requestId(userId + "-" + Instant.now().toEpochMilli()) // Benzersiz bir requestId
                .build();

        log.debug("Sending extraction request to LLM service: {}", prompt.substring(0, Math.min(100, prompt.length())) + "...");
        
        return llmService.processChatCompletion(request)
                .doOnSuccess(response -> log.info("Received AI response for extraction: length={}", 
                        response != null && response.getResponse() != null ? response.getResponse().length() : 0))
                .doOnError(e -> log.error("Error during LLM extraction request: {}", e.getMessage()))
                .flatMap(response -> parseExtractionResponse(userId, response, context, message));
    }

    public Mono<Void> processAndStoreUserInfo(String userId, String message, String context) {
        if (userId == null || message == null || message.trim().isEmpty()) {
            log.debug("Skipping extraction for empty/null message or userId");
            return Mono.empty();
        }
        
        log.info("Processing message for user {}: {}", userId, message.substring(0, Math.min(50, message.length())));
        
        // AI tabanlı bilgi çıkarımını kullan
        return extractUserInfo(userId, message, context)
                .doOnSubscribe(s -> log.debug("Starting extraction process for user {}", userId))
                .flatMap(extractedInfo -> {
                    if (extractedInfo == null || extractedInfo.getInformation() == null || extractedInfo.getInformation().isEmpty()) {
                        log.debug("No information extracted from message using AI for user {}", userId);
                        return Mono.empty();
                    }
                    
                    log.info("AI extracted user information: {}", extractedInfo.getInformation());
                    
                    // Timestamp'i burada ayarlayalım (client'ta kaybolabilir)
                    extractedInfo.setTimestamp(Instant.now());
                    
                    // User Memory servisine bilgileri gönder
                    log.debug("Sending extracted information to user-memory-service: userId={}, info={}", 
                            userId, extractedInfo.getInformation());
                    
                    return userMemoryClient.addMemoryEntry(extractedInfo)
                            .doOnSubscribe(s -> log.debug("Calling user-memory-service for user {}", userId))
                            .doOnSuccess(result -> log.info("Successfully stored AI-extracted information for user {}", userId))
                            .doOnError(error -> log.error("Failed to store AI-extracted information for user {}: {}", 
                                    userId, error.getMessage()))
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

    private Mono<ExtractedUserInfo> parseExtractionResponse(String userId, AIResponse response, String context, String originalMessage) {
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

    // Artık kullanılmayan regex tabanlı çıkarım metodu korundu, gerekirse yedek olarak kullanılabilir
    private String extractUserInformation(String message) {
        StringBuilder extractedInfo = new StringBuilder();
        
        // İsim çıkarma
        Matcher nameMatcher = NAME_PATTERN.matcher(message);
        if (nameMatcher.find() && nameMatcher.groupCount() >= 3) {
            extractedInfo.append("İsim: ").append(nameMatcher.group(3)).append(". ");
        }
        
        // Yaş çıkarma
        Matcher ageMatcher = AGE_PATTERN.matcher(message);
        if (ageMatcher.find() && ageMatcher.groupCount() >= 2) {
            extractedInfo.append("Yaş: ").append(ageMatcher.group(2)).append(". ");
        }
        
        // Konum çıkarma
        Matcher locationMatcher = LOCATION_PATTERN.matcher(message);
        if (locationMatcher.find() && locationMatcher.groupCount() >= 2) {
            extractedInfo.append("Konum: ").append(locationMatcher.group(2)).append(". ");
        }
        
        // Meslek çıkarma
        Matcher professionMatcher = PROFESSION_PATTERN.matcher(message);
        if (professionMatcher.find() && professionMatcher.groupCount() >= 2) {
            extractedInfo.append("Meslek: ").append(professionMatcher.group(2)).append(". ");
        }
        
        return extractedInfo.toString().trim();
    }
}
