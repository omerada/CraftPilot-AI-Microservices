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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserInformationExtractionService {
    private final UserMemoryClient userMemoryClient;
    private final LLMService llmService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Model değişikliği - Claude veya daha iyi bir model kullanılıyor
    @Value("${ai.model.extraction:anthropic/claude-3-haiku}")
    private String extractionModel;

    @Value("${extraction.timeout.seconds:30}")
    private int extractionTimeoutSeconds;
    
    @Value("${memory.timeout.seconds:10}")
    private int memoryTimeoutSeconds;
    
    @Value("${extraction.retry.max:3}")
    private int maxRetries;
    
    @Value("${extraction.retry.backoff.ms:500}")
    private long retryBackoffMs;

    @Value("${user-info-extraction.debug:true}")
    private boolean debugMode;

    @Value("${user-info-extraction.save-all-messages:false}")
    private boolean saveAllMessages;
    
    // Yeni eklenen: Geliştirmeler için anahtar sözcükleri tanımlama
    private static final List<String> INTERESTS_KEYWORDS = Arrays.asList(
            "ilgi", "hobi", "seviyorum", "beğeniyorum", "tutku", "zevk"
    );
    
    private static final List<String> PROFESSION_KEYWORDS = Arrays.asList(
            "meslek", "iş", "çalışıyorum", "uğraşıyorum", "yazılım", "mühendis", "doktor", "öğretmen", "öğrenci"
    );
    
    private static final List<String> TECH_KEYWORDS = Arrays.asList(
            "yazılım", "geliştir", "kod", "programla", "uygulama", "web", "mobil", "teknoloji", "bilgisayar"
    );
    
    private static final List<String> LOCATION_KEYWORDS = Arrays.asList(
            "istanbul", "ankara", "izmir", "bursa", "antalya", "adana", "konya", "trabzon", "türkiye", "şehir", "yaşıyorum", "oturuyorum"
    );

    public Mono<ExtractedUserInfo> extractUserInfo(String userId, String message) {
        if (userId == null || message == null || message.trim().isEmpty()) {
            log.debug("Skipping extraction for empty/null message or userId");
            return Mono.empty();
        }

        // Geliştirilmiş prompt
        String prompt = buildImprovedExtractionPrompt(message);
        log.debug("Extraction request created for userId={}, messageLength={}", userId, message.length());

        AIRequest extractionRequest = AIRequest.builder()
                .model(extractionModel)
                .prompt(prompt)
                .maxTokens(500)
                .temperature(0.2)  // Daha kesin sonuçlar için düşük sıcaklık
                .userId(userId)    // UserId'yi ekleyerek izleme kolaylığı
                .requestType("USER_INFORMATION_EXTRACTION") // Özel tip ile izleme
                .build();

        return llmService.processChatCompletion(extractionRequest)
                .timeout(Duration.ofSeconds(extractionTimeoutSeconds))
                .doOnSubscribe(s -> log.debug("Sending extraction request to AI for user {}", userId))
                .doOnSuccess(response -> {
                    String content = response != null && response.getResponse() != null 
                                    ? response.getResponse() : "";
                    log.info("Received AI response for extraction: length={}, content snippet: {}", 
                            content.length(), 
                            content.length() > 0 ? content.substring(0, Math.min(100, content.length())) : "EMPTY");
                })
                .onErrorResume(e -> {
                    log.error("Error while extracting user info: {} (Type: {})", e.getMessage(), e.getClass().getName());
                    // JSON formatında hata yanıtı için
                    return Mono.just(AIResponse.builder()
                            .response("{\"information\": \"EXTRACTION_ERROR\", \"error\": \"" + e.getMessage().replaceAll("\"", "'") + "\"}")
                            .build());
                })
                .flatMap(response -> parseEnhancedExtractionResponse(userId, response, message))
                .retryWhen(Retry.backoff(maxRetries, Duration.ofMillis(retryBackoffMs))
                        .filter(e -> !(e instanceof IllegalArgumentException))
                        .doBeforeRetry(signal -> 
                            log.warn("Retrying extraction after error: {}, attempt: {}", 
                                    signal.failure().getMessage(), signal.totalRetries() + 1)))
                .doFinally(s -> LoggingUtils.clearRequestContext());
    }

    // Geliştirilmiş prompt oluşturma - daha iyi sonuçlar için
    private String buildImprovedExtractionPrompt(String message) {
        return """
                Aşağıdaki kullanıcı mesajından anlamlı bilgileri çıkartıp JSON formatında döndür:
                
                "%s"
                
                Mesajdan kullanıcının adı, yaşadığı yer, ilgi alanları, mesleği, teknoloji bilgisi, 
                tercihleri ve diğer kişisel bilgileri tespit etmeye çalış.
                
                ÖNEMLİ: 
                1. Eğer mesajda hiçbir anlamlı kişisel bilgi yoksa, boş döndürmek yerine mesajın 
                   ana konusunu veya amacını belirt.
                2. Mutlaka JSON formatında yanıt ver.
                3. Kesin bilgi yoksa tahmin yürütme.
                
                Örnek yanıt formatı:
                {
                  "bilgiler": [
                    "Kullanıcı yazılım geliştirme ile ilgileniyor",
                    "Kullanıcı CraftPilot adlı bir proje üzerinde çalışıyor"
                  ]
                }
                
                Sadece JSON formatında cevap ver, hiçbir açıklama ya da ek metin kullanma.
                """.formatted(message);
    }

    // Geliştirilmiş yanıt ayrıştırma
    private Mono<ExtractedUserInfo> parseEnhancedExtractionResponse(String userId, AIResponse response, String originalMessage) {
        if (response == null || response.getResponse() == null || response.getResponse().isEmpty()) {
            log.warn("AI response is null or empty for user {}", userId);
            
            // AI yanıtı boş ise gelişmiş rule-based extraction kullan
            String extractedInfo = performAdvancedRuleBasedExtraction(originalMessage);
            if (extractedInfo != null && !extractedInfo.isEmpty() && !extractedInfo.equals("Mesajdan bilgi çıkarılamadı")) {
                log.info("Rule-based extraction successful for user {}: {}", userId, extractedInfo);
                ExtractedUserInfo info = new ExtractedUserInfo();
                info.setUserId(userId);
                info.setInformation(extractedInfo);
                info.setSource("Kural tabanlı çıkarım");
                info.setContext("Mesaj analizi: " + shortenMessage(originalMessage));
                info.setTimestamp(Instant.now());
                return Mono.just(info);
            }
            
            // Gelişmiş kural tabanlı çıkarım da başarısız olduysa varsayılan bilgiyi döndür
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
                log.warn("Non-JSON response received: {}", jsonResponse.substring(0, Math.min(100, jsonResponse.length())));
                jsonResponse = extractJsonFromText(jsonResponse);
                log.debug("Extracted JSON from text: {}", jsonResponse);
            }
            
            // JSON içindeki bilgileri çıkart
            JsonNode root = objectMapper.readTree(jsonResponse);
            
            // İki farklı yanıt formatını destekle
            JsonNode bilgilerNode = root.get("bilgiler");
            JsonNode informationNode = root.get("information");
            
            if (bilgilerNode != null && bilgilerNode.isArray()) {
                StringBuilder combinedInfo = new StringBuilder();
                
                // Diziden tüm bilgileri al
                for (JsonNode item : bilgilerNode) {
                    if (item.isTextual() && !item.asText().isEmpty()) {
                        if (combinedInfo.length() > 0) {
                            combinedInfo.append(". ");
                        }
                        combinedInfo.append(item.asText());
                    }
                }
                
                String extractedInfo = combinedInfo.toString();
                
                // Boş ya da anlamsız bilgi kontrolü
                if (extractedInfo.isEmpty()) {
                    log.warn("Empty array or no valid items in 'bilgiler' array for user {}", userId);
                    String ruleBasedInfo = performAdvancedRuleBasedExtraction(originalMessage);
                    ExtractedUserInfo basicInfo = new ExtractedUserInfo();
                    basicInfo.setUserId(userId);
                    basicInfo.setInformation(ruleBasedInfo);
                    basicInfo.setSource("Kural tabanlı çıkarım (fallback)");
                    basicInfo.setContext("Mesaj: " + shortenMessage(originalMessage));
                    basicInfo.setTimestamp(Instant.now());
                    return Mono.just(basicInfo);
                }
                
                ExtractedUserInfo info = new ExtractedUserInfo();
                info.setUserId(userId);
                info.setInformation(extractedInfo);
                info.setSource("AI çıkarımı");
                info.setContext("Mesaj analizi: " + shortenMessage(originalMessage));
                info.setTimestamp(Instant.now());
                
                log.info("Successfully extracted information from 'bilgiler' array for user {}: {}", 
                        userId, shortenMessage(extractedInfo));
                return Mono.just(info);
            } 
            // Eski format kontrolü
            else if (informationNode != null && informationNode.isTextual()) {
                String extractedInfo = informationNode.asText();
                
                // Anlamsız ya da işlenemez yanıt kontrolü
                if (extractedInfo.equals("EXTRACTION_ERROR") || 
                    extractedInfo.equals("NO_INFORMATION") || 
                    extractedInfo.isEmpty()) {
                    
                    log.warn("AI returned non-useful extraction result: {}", extractedInfo);
                    String ruleBasedInfo = performAdvancedRuleBasedExtraction(originalMessage);
                    
                    ExtractedUserInfo basicInfo = new ExtractedUserInfo();
                    basicInfo.setUserId(userId);
                    basicInfo.setInformation(ruleBasedInfo);
                    basicInfo.setSource("Kural tabanlı çıkarım (fallback)");
                    basicInfo.setContext("Mesaj: " + shortenMessage(originalMessage));
                    basicInfo.setTimestamp(Instant.now());
                    return Mono.just(basicInfo);
                }
                
                ExtractedUserInfo info = new ExtractedUserInfo();
                info.setUserId(userId);
                info.setInformation(extractedInfo);
                info.setSource("AI çıkarımı (eski format)");
                info.setContext("Mesaj analizi: " + shortenMessage(originalMessage));
                info.setTimestamp(Instant.now());
                
                log.info("Successfully parsed extraction response from 'information' field for user {}: {}", 
                      userId, shortenMessage(extractedInfo));
                return Mono.just(info);
            } 
            // Format tanınamadı, tüm JSON'ı anlamlı metne dönüştür
            else {
                log.warn("Unexpected JSON structure received for user {}, attempting to convert entire JSON to text", userId);
                
                StringBuilder extractedInfo = new StringBuilder();
                root.fields().forEachRemaining(entry -> {
                    String key = entry.getKey();
                    JsonNode value = entry.getValue();
                    
                    if (!key.equals("error") && !value.isNull() && !value.asText().isEmpty()) {
                        if (extractedInfo.length() > 0) {
                            extractedInfo.append(". ");
                        }
                        
                        if (value.isTextual()) {
                            extractedInfo.append(key).append(": ").append(value.asText());
                        } else if (value.isArray()) {
                            extractedInfo.append(key).append(": ");
                            AtomicBoolean first = new AtomicBoolean(true);
                            value.forEach(item -> {
                                if (!first.get()) {
                                    extractedInfo.append(", ");
                                }
                                extractedInfo.append(item.asText());
                                first.set(false);
                            });
                        } else {
                            extractedInfo.append(key).append(": ").append(value.toString());
                        }
                    }
                });
                
                String finalInfo = extractedInfo.toString();
                if (finalInfo.isEmpty()) {
                    finalInfo = performAdvancedRuleBasedExtraction(originalMessage);
                }
                
                ExtractedUserInfo info = new ExtractedUserInfo();
                info.setUserId(userId);
                info.setInformation(finalInfo);
                info.setSource("AI çıkarımı (dönüştürülmüş)");
                info.setContext("Mesaj analizi: " + shortenMessage(originalMessage));
                info.setTimestamp(Instant.now());
                
                log.info("Converted JSON to text for user {}: {}", userId, shortenMessage(finalInfo));
                return Mono.just(info);
            }
            
        } catch (Exception e) {
            log.error("Error parsing extraction response: {}", e.getMessage(), e);
            
            // Hata durumunda rule-based extraction kullan
            String extractedInfo = performAdvancedRuleBasedExtraction(originalMessage);
            
            ExtractedUserInfo errorInfo = new ExtractedUserInfo();
            errorInfo.setUserId(userId);
            errorInfo.setInformation(extractedInfo);
            errorInfo.setSource("Kural tabanlı çıkarım (hata sonrası)");
            errorInfo.setContext("Hata: " + e.getMessage());
            errorInfo.setTimestamp(Instant.now());
            return Mono.just(errorInfo);
        }
    }

    // Gelişmiş kural tabanlı çıkarım
    private String performAdvancedRuleBasedExtraction(String message) {
        if (message == null || message.trim().isEmpty()) {
            return "Mesajdan bilgi çıkarılamadı";
        }
        
        StringBuilder extracted = new StringBuilder();
        String lowerMessage = message.toLowerCase();
        
        // İlgi alanları tespiti
        for (String keyword : INTERESTS_KEYWORDS) {
            if (lowerMessage.contains(keyword)) {
                int index = lowerMessage.indexOf(keyword);
                String context = extractContextAroundKeyword(message, index, 20);
                extracted.append("Kullanıcının ilgi alanı: ").append(context).append(". ");
                break;
            }
        }
        
        // Meslek tespiti
        for (String keyword : PROFESSION_KEYWORDS) {
            if (lowerMessage.contains(keyword)) {
                int index = lowerMessage.indexOf(keyword);
                String context = extractContextAroundKeyword(message, index, 20);
                extracted.append("Kullanıcının mesleği/uğraşı: ").append(context).append(". ");
                break;
            }
        }
        
        // Teknoloji tespiti
        for (String keyword : TECH_KEYWORDS) {
            if (lowerMessage.contains(keyword)) {
                int index = lowerMessage.indexOf(keyword);
                String context = extractContextAroundKeyword(message, index, 25);
                extracted.append("Kullanıcı teknoloji ile ilgili: ").append(context).append(". ");
                break;
            }
        }
        
        // Şehir/yer çıkarma
        for (String city : LOCATION_KEYWORDS) {
            if (lowerMessage.contains(city)) {
                extracted.append("Kullanıcı ").append(city.substring(0, 1).toUpperCase() + city.substring(1)).append(" ile ilgili. ");
                break;
            }
        }
        
        // İsim çıkarma girişimi
        if (lowerMessage.contains("adım") || lowerMessage.contains("ismim")) {
            String[] parts = message.split("\\s+");
            for (int i = 0; i < parts.length; i++) {
                if ((parts[i].equalsIgnoreCase("adım") || parts[i].equalsIgnoreCase("ismim")) && i < parts.length - 1) {
                    String name = parts[i+1].replaceAll("[^a-zA-ZğüşıöçĞÜŞİÖÇ]", "");
                    if (!name.isEmpty()) {
                        extracted.append("Kullanıcının adı ").append(name).append(". ");
                    }
                    break;
                }
            }
        }
        
        // Proje/ürün tespiti
        if (lowerMessage.contains("proje") || lowerMessage.contains("ürün") || lowerMessage.contains("uygulama")) {
            Pattern pattern = Pattern.compile("\\b([a-zA-ZğüşıöçĞÜŞİÖÇ][a-zA-ZğüşıöçĞÜŞİÖÇ0-9]*(?:[Pp]roje|[Uu]ygulama|[Üü]rün|[Pp]roduct|[Aa]pp))\\b");
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                extracted.append("Kullanıcının projesi: ").append(matcher.group(1)).append(". ");
            } else if (message.contains("craftpilot") || message.contains("craft pilot")) {
                extracted.append("Kullanıcı CraftPilot ile ilgileniyor. ");
            }
        }
        
        // Eğer hiçbir şey çıkarılamazsa, mesajın kendisinden anlamlı bilgi çıkarmaya çalış
        if (extracted.length() == 0) {
            if (lowerMessage.length() > 15) {
                // Mesajın ana konusu nedir?
                extracted.append("Kullanıcı mesajında '").append(message.substring(0, Math.min(50, message.length()))).append("' konusundan bahsediyor. ");
            } else {
                extracted.append("Mesajdan bilgi çıkarılamadı");
            }
        }
        
        return extracted.toString().trim();
    }
    
    // Anahtar kelimenin etrafındaki metni çıkarma
    private String extractContextAroundKeyword(String text, int keywordIndex, int contextSize) {
        int start = Math.max(0, keywordIndex - contextSize);
        int end = Math.min(text.length(), keywordIndex + contextSize);
        
        // Kelime sınırlarına göre ayarlama
        while (start > 0 && Character.isLetterOrDigit(text.charAt(start))) {
            start--;
        }
        while (end < text.length() && Character.isLetterOrDigit(text.charAt(end))) {
            end++;
        }
        
        return text.substring(start, end).trim();
    }

    // Mesajdan JSON çıkarma - geliştirilmiş
    private String extractJsonFromText(String text) {
        int jsonStart = text.indexOf('{');
        int jsonEnd = text.lastIndexOf('}');
        
        if (jsonStart >= 0 && jsonEnd > jsonStart) {
            return text.substring(jsonStart, jsonEnd + 1);
        }
        
        // Hiç JSON bulunamazsa, uyumlu bir format döndür
        return "{\"bilgiler\": [\"Mesajdan çıkarılan bilgi bulunamadı\"]}";
    }
    
    // Geliştirilmiş mesaj kısaltma
    private String shortenMessage(String message) {
        if (message == null) return "";
        int maxLength = 50; // Daha uzun snippet
        if (message.length() <= maxLength) return message;
        return message.substring(0, maxLength) + "...";
    }

    // Bilgi işleme ve saklama - geliştirilmiş
    public Mono<Void> processAndStoreUserInfo(String userId, String message) {
        // UserId kontrolü
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
                            userId, LoggingUtils.truncateForLogging(extractedInfo.getInformation(), 100));
                    }
                })
                .onErrorResume(e -> {
                    int attempt = retryCount.getAndIncrement();
                    if (attempt < maxRetries) {
                        log.warn("Retrying extraction after error: {}, attempt: {}", e.getMessage(), attempt);
                        return Mono.delay(Duration.ofSeconds(1)).then(Mono.empty());
                    }
                    log.error("Failed to extract user info after {} attempts: {}", maxRetries, e.getMessage());
                    
                    // Gelişmiş varsayılan bilgi oluştur
                    ExtractedUserInfo defaultInfo = createDefaultExtractedInfo(userId, message);
                    // Kural tabanlı çıkarım dene
                    defaultInfo.setInformation(performAdvancedRuleBasedExtraction(message));
                    return Mono.just(defaultInfo);
                })
                .flatMap(extractedInfo -> {
                    if (extractedInfo == null) {
                        log.warn("Extraction failed with null result for user {}", userId);
                        return Mono.empty();
                    }
                    
                    // Anlamlı bilgi var mı kontrol et - geliştirilmiş
                    String info = extractedInfo.getInformation();
                    if (info == null || info.isEmpty() || 
                        "NO_INFORMATION".equals(info) || 
                        "EXTRACTION_ERROR".equals(info) ||
                        "PARSING_ERROR".equals(info) ||
                        "INVALID_RESPONSE_FORMAT".equals(info) ||
                        "Mesajdan bilgi çıkarılamadı".equals(info) ||
                        "Kullanıcı mesaj gönderdi".equals(info)) {
                        
                        // Anlamlı bilgi yoksa, belleğe kaydetme
                        log.info("No meaningful information extracted for user {}, skipping memory storage. Content: '{}'", 
                                userId, info != null ? info : "null");
                        return Mono.empty();
                    }

                    // Sadece anlamlı bilgi çıkarıldığında memory'ye kaydet
                    extractedInfo.setUserId(userId);
                    extractedInfo.setTimestamp(Instant.now());
                    
                    // Daha anlamlı context ekle
                    if (extractedInfo.getContext() == null || extractedInfo.getContext().isEmpty()) {
                        extractedInfo.setContext("Mesajdan çıkarıldı: " + 
                            LoggingUtils.truncateForLogging(message, 30));
                    }

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
        
        // Basit bilgi çıkarımı dene
        String extractedInfo = performAdvancedRuleBasedExtraction(message);
        
        // Eğer anlamlı bilgi çıkartılamazsa varsayılan mesaj kullan
        if (extractedInfo == null || extractedInfo.isEmpty() || extractedInfo.equals("Mesajdan bilgi çıkarılamadı")) {
            if (message.contains("craftpilot")) {
                defaultInfo.setInformation("Kullanıcı CraftPilot projesi hakkında konuşuyor");
            } else {
                defaultInfo.setInformation("Kullanıcı mesaj gönderdi");
            }
        } else {
            defaultInfo.setInformation(extractedInfo);
        }
        
        defaultInfo.setSource("Varsayılan çıkarım");
        defaultInfo.setContext("Mesaj: " + shortenMessage(message));
        defaultInfo.setTimestamp(Instant.now());
        return defaultInfo;
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

    /**
     * Kullanıcı mesajından bilgi çıkarmak için AI istek içeriğini oluşturur
     */
    private String buildExtractionPrompt(String message) {
        return """
                Lütfen bu kullanıcı mesajından kullanıcı hakkında bilgileri çıkart:
                
                "%s"
                
                Bu mesajdan kullanıcının adı, yaşadığı yer, ilgi alanları, mesleği, tercih ettiği şeyler gibi 
                kişisel bilgilerini çıkarmaya çalış.
                
                Format:
                - Eğer mesajda kullanıcının adı varsa: "Kullanıcının adı [isim]"
                - Eğer mesajda yaşadığı yer varsa: "Kullanıcı [yer] yaşıyor"
                - Eğer mesajda başka bir kişisel bilgi varsa, benzer formatta belirt.
                
                Yanıtı boş bırakma. Eğer hiçbir bilgi çıkaramıyorsan, "Çıkarılabilecek bilgi bulunamadı" yaz.
                Sadece kesin olan bilgileri belirt, tahmin yürütme.
                """.formatted(message);
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
    
    /**
     * AI tarafından döndürülen yanıtı işleyerek anlamlı bilgiye dönüştürür
     */
    private ExtractedUserInfo processExtractionResponse(String aiResponse, String originalMessage) {
        log.info("Processing AI extraction response: {}", LoggingUtils.truncateForLogging(aiResponse, 100));
        
        ExtractedUserInfo extractedInfo = new ExtractedUserInfo();
        
        // AI yanıtı boş veya null ise, daha akıllı bir varsayılan yanıt oluştur
        if (aiResponse == null || aiResponse.trim().isEmpty() || aiResponse.equals("EMPTY")) {
            // Basit bir bilgi çıkarma denemesi yap
            String extractedDefault = attemptBasicExtraction(originalMessage);
            extractedInfo.setInformation(extractedDefault);
            log.info("AI yanıtı boş, basit çıkarım yapıldı: {}", extractedDefault);
        } else {
            extractedInfo.setInformation(aiResponse.trim());
        }
        
        extractedInfo.setSource("AI çıkarımı");
        return extractedInfo;
    }

    /**
     * AI yanıtı boş olduğunda basit bir bilgi çıkarma dener
     */
    private String attemptBasicExtraction(String message) {
        // Basit düzenli ifadelerle bilgi çıkarma
        StringBuilder extracted = new StringBuilder();
        
        // İsim çıkarma (basit örnek)
        if (message.toLowerCase().contains("adım") || message.toLowerCase().contains("ismim")) {
            String[] parts = message.split("\\s+");
            for (int i = 0; i < parts.length; i++) {
                if ((parts[i].equalsIgnoreCase("adım") || parts[i].equalsIgnoreCase("ismim")) && i < parts.length - 1) {
                    extracted.append("Kullanıcının adı ").append(parts[i+1]).append(". ");
                    break;
                }
            }
        }
        
        // Şehir/yer çıkarma (basit örnek)
        String[] cities = {"ankara", "istanbul", "izmir", "bursa", "antalya"}; // Örnekler
        String lowerMessage = message.toLowerCase();
        for (String city : cities) {
            if (lowerMessage.contains(city)) {
                extracted.append("Kullanıcı ").append(city.substring(0, 1).toUpperCase() + city.substring(1)).append("'da yaşıyor. ");
                break;
            }
        }
        
        return extracted.length() > 0 ? extracted.toString() : "Mesajdan bilgi çıkarılamadı";
    }
}
