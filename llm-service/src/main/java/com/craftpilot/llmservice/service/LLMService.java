package com.craftpilot.llmservice.service;

import com.craftpilot.llmservice.model.AIRequest;
import com.craftpilot.llmservice.model.AIResponse;
import com.craftpilot.llmservice.model.StreamResponse;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.Disposable;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.Duration;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.ClientResponse;
import com.craftpilot.llmservice.exception.APIException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;
import java.util.Set;
import com.craftpilot.llmservice.exception.ValidationException;

import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class LLMService {
    private final WebClient openRouterWebClient; 
    private final ObjectMapper objectMapper;
    
    // Model başına maksimum token limitleri
    private static final Map<String, Integer> MODEL_TOKEN_LIMITS = Map.ofEntries(
        Map.entry("google/gemini-2.0-flash-lite-001", 30000),
        Map.entry("google/gemini-pro", 30000),
        Map.entry("google/palm-2-codechat-bison", 8000),
        Map.entry("google/palm-2-chat-bison", 8000),
        Map.entry("anthropic/claude-3-haiku", 48000),
        Map.entry("anthropic/claude-3-sonnet", 180000),
        Map.entry("anthropic/claude-3-opus", 180000),
        Map.entry("anthropic/claude-2", 100000),
        Map.entry("openai/gpt-4", 8000),
        Map.entry("openai/gpt-4-turbo", 128000),
        Map.entry("openai/gpt-3.5-turbo", 16000),
        Map.entry("meta-llama/llama-3-70b-instruct", 8000),
        Map.entry("meta-llama/llama-3-8b-instruct", 8000),
        Map.entry("mistral/mistral-large", 32000),
        Map.entry("mistral/mistral-medium", 32000),
        Map.entry("mistral/mistral-small", 32000)
    );
    // Varsayılan token limiti
    private static final int DEFAULT_TOKEN_LIMIT = 4000;

    // Add these constants for table detection
    private static final int MAX_CONSECUTIVE_WHITESPACE = 3; // Daha agresif whitespace filtreleme
    private static final int MAX_CHUNK_SIZE = 1000; // Daha küçük chunk boyutu
    private static final String TABLE_MARKER = "|";
    private static final String TABLE_DELIMITER_MARKER = "|-";
    private static final Pattern TABLE_ROW_PATTERN = Pattern.compile("\\|.+\\|");
    private static final Pattern TABLE_HEADER_PATTERN = Pattern.compile("\\|[\\s-:]*\\|");
    private static final boolean CONVERT_TABLES_TO_LISTS = true; // Tabloları liste formatına dönüştür

    // Tablo işleme için ekstra değişkenler
    private StringBuilder tableBuffer = new StringBuilder();
    private boolean isBufferingTable = false;
    private int tableRowCount = 0;
    private static final int MIN_TABLE_ROWS_TO_BUFFER = 2; // En az bu kadar satır birikmeden gönderme

    public Mono<AIResponse> processChatCompletion(AIRequest request) {
        // Request validasyonu
        if (request.getRequestId() == null) {
            request.setRequestId(UUID.randomUUID().toString());
        }
        
        Map<String, Object> requestBody = createRequestBody(request);
        log.debug("Chat tamamlama isteği gönderiliyor: {}", requestBody);
        
        return openRouterWebClient.post()
            .uri("/chat/completions")
            .bodyValue(requestBody)
            .headers(headers -> {
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("Accept", "application/json");
            })
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, response -> 
                response.bodyToMono(String.class)
                    .flatMap(error -> Mono.error(new APIException("Client error: " + error))))
            .onStatus(HttpStatusCode::is5xxServerError, response -> 
                response.bodyToMono(String.class)
                    .flatMap(error -> Mono.error(new APIException("Server error: " + error))))
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
            .doOnNext(response -> log.debug("OpenRouter yanıtı alındı: {}", response))
            .map(response -> mapToAIResponse(response, request))
            .timeout(Duration.ofSeconds(60))
            .doOnError(e -> log.error("Chat completion error: {}", e.getMessage(), e))
            .onErrorResume(e -> {
                log.error("Hata yakalandı: {}", e.getMessage());
                return Mono.just(AIResponse.error("AI servisi hatası: " + e.getMessage()));
            });
    }

    public Mono<AIResponse> processCodeCompletion(AIRequest request) {
        // Code completion için özel model seçimi
        return callOpenRouter("chat/completions", request)  // Başlangıçtaki slash kaldırıldı
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
        
        log.info("Starting streaming request for model: {}, requestBody: {}", request.getModel(), requestBody);
        
        // OpenRouter API zaman aşımı süresini artır
        Duration requestTimeout = Duration.ofSeconds(60);
        Duration keepAliveInterval = Duration.ofSeconds(5);
        
        // Define timeout response explicitly to avoid type inference issues
        StreamResponse timeoutResponse = StreamResponse.builder()
            .content("Stream timeout occurred after 90 seconds")
            .done(true)
            .error(true)
            .build();
        
        // Liste oluşturarak tam tür bilgisini koruyoruz
        List<StreamResponse> timeoutList = Collections.singletonList(timeoutResponse);
        Flux<StreamResponse> timeoutFlux = Flux.fromIterable(timeoutList);
        
        // OpenRouter'dan gelen JSON parçalarını birleştirmek için
        StringBuilder jsonBuffer = new StringBuilder();
        
        // Tabloları yeniden başlatmak için
        tableBuffer.setLength(0);
        isBufferingTable = false;
        tableRowCount = 0;
        
        // Explicit generic type parameter to inform Java compiler about the type we're creating
        return Flux.<StreamResponse>create(sink -> {
            log.debug("Creating stream flux for model: {}", request.getModel());
            
            // İstemciye periyodik ping göndermek için timer başlat
            Disposable keepAliveTicker = Flux.interval(keepAliveInterval)
                .doOnNext(tick -> {
                    log.debug("Sending keep-alive ping, tick: {}", tick);
                    sink.next(StreamResponse.builder()
                        .content("")
                        .done(false)
                        .ping(true)  // Ping olduğunu belirt
                        .build());
                })
                .subscribe();
            
            // Asıl API isteğini başlat
            openRouterWebClient.post()
                .uri("/chat/completions")
                .bodyValue(requestBody)
                .headers(headers -> {
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.set("Accept", "text/event-stream");
                })
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(requestTimeout)
                .doOnSubscribe(s -> log.debug("Subscribed to OpenRouter stream"))
                .doOnComplete(() -> {
                    log.info("OpenRouter stream completed successfully");
                    keepAliveTicker.dispose();
                    
                    // Eğer biriken JSON varsa, son bir işleme deneyin
                    if (jsonBuffer.length() > 0) {
                        try {
                            String finalJson = jsonBuffer.toString();
                            extractAndSendContent(finalJson, sink);
                            jsonBuffer.setLength(0);
                        } catch (Exception e) {
                            log.warn("Could not process final buffer: {}", e.getMessage());
                        }
                    }
                    
                    // Biriken tablo içeriğini gönder
                    flushTableBuffer(sink);
                    
                    sink.next(StreamResponse.builder()
                        .content("")
                        .done(true)
                        .build());
                    sink.complete();
                })
                .doOnCancel(() -> {
                    log.warn("OpenRouter stream was cancelled");
                    keepAliveTicker.dispose();
                    sink.complete();
                })
                .doOnError(e -> {
                    log.error("OpenRouter stream error: {}", e.getMessage(), e);
                    keepAliveTicker.dispose();
                    sink.next(StreamResponse.builder()
                        .content("Error: " + e.getMessage())
                        .done(true)
                        .error(true)
                        .build());
                    sink.complete();
                })
                .subscribe(chunk -> {
                    // Always log the raw chunk for debugging
                    log.info("Raw OpenRouter chunk: {}", chunk);
                    
                    if (chunk == null || chunk.isEmpty()) {
                        log.debug("Empty chunk received, skipping");
                        return;
                    }
                    
                    // SSE veri formatını işle
                    if (chunk.startsWith("data:")) {
                        String data = chunk.substring(5).trim();
                        
                        if ("[DONE]".equals(data)) {
                            log.debug("Received completion marker [DONE]");
                            sink.next(StreamResponse.builder()
                                .content("")
                                .done(true)
                                .build());
                            return;
                        }

                        // Chunk'ın içeriğini JSON buffer'a ekle
                        try {
                            // Önce "content" içindeki JSON string'i çıkarmaya çalış
                            JsonNode outerNode = objectMapper.readTree(data);
                            if (outerNode.has("content")) {
                                // Dış JSON'dan content string'ini çıkar
                                String content = outerNode.get("content").asText();
                                
                                // Content boş değilse işle
                                if (content != null && !content.isEmpty()) {
                                    // Buffer'a ekle
                                    jsonBuffer.append(content);
                                    
                                    try {
                                        // Tam bir JSON oluştu mu deneyin
                                        extractAndSendContent(jsonBuffer.toString(), sink);
                                        // Başarılı ise buffer'ı temizle
                                        jsonBuffer.setLength(0);
                                    } catch (Exception e) {
                                        // JSON eksik olabilir, buffer'a eklemeye devam
                                        log.debug("Current buffer is incomplete JSON, continuing collection...");
                                    }
                                }
                            } else {
                                // Content yoksa diğer formatlara bak
                                checkAndSendStandardFormats(outerNode, sink);
                            }
                        } catch (Exception e) {
                            log.debug("Failed to parse JSON, collecting as raw: {}", e.getMessage());
                            
                            // JSON parsing hatası, belki de parçalı JSON
                            jsonBuffer.append(data);
                            
                            try {
                                // Tamam mı kontrol et
                                extractAndSendContent(jsonBuffer.toString(), sink);
                                jsonBuffer.setLength(0); // Başarılıysa temizle
                            } catch (Exception e2) {
                                // Hala eksik
                                log.debug("Buffer still incomplete");
                            }
                        }
                    } else if (chunk.startsWith(":")) {
                        // Yorum veya keep-alive, görmezden gel
                        log.debug("Comment line received: {}", chunk);
                    } else {
                        // Non-SSE format, may be direct content
                        jsonBuffer.append(chunk);
                        
                        try {
                            // Topladığımız verileri işlemeyi dene
                            String bufferContent = jsonBuffer.toString();
                            extractAndSendContent(bufferContent, sink);
                            jsonBuffer.setLength(0);
                        } catch (Exception e) {
                            log.debug("Buffer not yet complete: {}", e.getMessage());
                        }
                    }
                });
        }, FluxSink.OverflowStrategy.BUFFER)
        .doOnRequest(n -> log.debug("Requested {} items from stream", n))
        .onBackpressureBuffer(256)
        .timeout(Duration.ofSeconds(90), timeoutFlux)
        .doOnTerminate(() -> log.info("Stream terminated"));
    }
    
    /**
     * JSON verisinden içerik çıkarıp sink'e gönderir
     */
    private void extractAndSendContent(String jsonText, FluxSink<StreamResponse> sink) throws Exception {
        // JSON'ı parse et
        JsonNode jsonNode = objectMapper.readTree(jsonText);
        
        // İçeriği çıkar ve gönder
        checkAndSendStandardFormats(jsonNode, sink);
    }
    
    /**
     * Standart OpenAI ve diğer formatları kontrol edip içeriği çıkarır
     */
    private void checkAndSendStandardFormats(JsonNode jsonNode, FluxSink<StreamResponse> sink) {
        boolean contentSent = false;

        // OpenAI format: choices[0].delta.content
        if (jsonNode.has("choices") && jsonNode.get("choices").isArray()) {
            JsonNode choices = jsonNode.get("choices");
            if (choices.size() > 0) {
                JsonNode choice = choices.get(0);
                
                // Delta format
                if (choice.has("delta") && choice.get("delta").has("content")) {
                    String content = choice.get("delta").get("content").asText();
                    if (content != null && !content.isEmpty()) {
                        content = filterAndProcessContent(content, sink);
                        log.debug("Extracted delta.content: {}", 
                            content.length() > 30 ? content.substring(0, 30) + "..." : content);
                        
                        if (!content.isEmpty()) {
                            sink.next(StreamResponse.builder()
                                .content(content)
                                .done(false)
                                .build());
                        }
                        contentSent = true;
                    }
                }
                
                // Text format
                if (!contentSent && choice.has("text")) {
                    String content = choice.get("text").asText();
                    if (content != null && !content.isEmpty()) {
                        content = filterAndProcessContent(content, sink);
                        log.debug("Extracted text: {}", content);
                        
                        if (!content.isEmpty()) {
                            sink.next(StreamResponse.builder()
                                .content(content)
                                .done(false)
                                .build());
                        }
                        contentSent = true;
                    }
                }
                
                // Message format
                if (!contentSent && choice.has("message") && choice.get("message").has("content")) {
                    String content = choice.get("message").get("content").asText();
                    if (content != null && !content.isEmpty()) {
                        content = filterAndProcessContent(content, sink);
                        log.debug("Extracted message.content: {}", content);
                        
                        if (!content.isEmpty()) {
                            sink.next(StreamResponse.builder()
                                .content(content)
                                .done(false)
                                .build());
                        }
                        contentSent = true;
                    }
                }
            }
        }
        
        // Diğer formatlar
        if (!contentSent) {
            // Direct content field
            if (jsonNode.has("content") && jsonNode.get("content").isTextual()) {
                String content = jsonNode.get("content").asText();
                if (content != null && !content.isEmpty()) {
                    content = filterAndProcessContent(content, sink);
                    
                    if (content.startsWith("{") && content.endsWith("}")) {
                        // Bu durumda content alanı başka bir JSON - bunu parse etmeye çalışalım
                        try {
                            JsonNode innerNode = objectMapper.readTree(content);
                            // İç JSON'ı da kontrol et
                            checkAndSendStandardFormats(innerNode, sink);
                            contentSent = true;
                        } catch (Exception e) {
                            log.debug("Could not parse inner content as JSON, sending as text");
                        }
                    }
                    
                    if (!contentSent && !content.isEmpty()) {
                        log.debug("Using direct content: {}", 
                            content.length() > 30 ? content.substring(0, 30) + "..." : content);
                        sink.next(StreamResponse.builder()
                            .content(content)
                            .done(false)
                            .build());
                        contentSent = true;
                    }
                }
            }
            
            // Hiçbir şekilde içerik çıkaramadıysak...
            if (!contentSent) {
                log.warn("Could not extract content from JSON: {}", jsonNode);
            }
        }
    }
    
    /**
     * İçeriği filtrele ve işle - tablo içeriği için özel işleme yapar
     */
    private String filterAndProcessContent(String content, FluxSink<StreamResponse> sink) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        
        // Aşırı boşlukları temizle - Gemini modelinin çıktılarında sorun yaratıyor
        String filtered = content.replaceAll("\\s{" + MAX_CONSECUTIVE_WHITESPACE + ",}", " ");
        
        // Tablo işaretleri içeriyor mu kontrol et
        boolean hasTableMarker = filtered.contains("|");
        boolean mightBePartOfTable = hasTableMarker || 
                                     filtered.trim().equals("**") || 
                                     filtered.matches("\\s*\\w+\\s*") && isBufferingTable;
        
        // Tablo işleme modunda mıyız?
        if (isBufferingTable) {
            if (mightBePartOfTable) {
                // Tablo içeriğine ekle
                tableBuffer.append(filtered);
                
                // Satır tamamlandı mı kontrol et (satır sonu karakteri ile)
                if (filtered.contains("\n")) {
                    tableRowCount++;
                }
                
                // Tablo yeterince birikti mi veya tamamlandı mı kontrol et
                if (tableRowCount >= MIN_TABLE_ROWS_TO_BUFFER || filtered.endsWith("\n\n")) {
                    // Tabloyu işle ve gönder
                    String tableContent = processAndReformatTable(tableBuffer.toString());
                    
                    // Bufferi temizle ve tablo modu kapat (buffer'da kalabilecek veriler sonraki içeriklerde gönderilecek)
                    tableBuffer.setLength(0);
                    tableRowCount = 0;
                    isBufferingTable = false;
                    
                    // İşlenmiş tabloyu döndür
                    return tableContent;
                }
                
                // Henüz tablo tamamlanmadı, boş dön (birikmeye devam)
                return "";
            } else {
                // Tablo içeriği değil, bufferi işle ve gönder
                String tableContent = "";
                if (tableBuffer.length() > 0) {
                    tableContent = processAndReformatTable(tableBuffer.toString());
                    tableBuffer.setLength(0);
                }
                
                // Tablo modunu kapat
                tableRowCount = 0;
                isBufferingTable = false;
                
                // İşlenmiş tabloyu ve mevcut içeriği birleştirerek döndür
                return tableContent + filtered;
            }
        } else if (hasTableMarker || detectTableStart(filtered)) {
            // Yeni bir tablo başlangıcı tespit edildi
            isBufferingTable = true;
            tableBuffer.append(filtered);
            
            // Satır tamamlandı mı kontrol et
            if (filtered.contains("\n")) {
                tableRowCount++;
            }
            
            // İlk chunk'ta tabloyu tamamlayabilir miyiz?
            if (tableRowCount >= MIN_TABLE_ROWS_TO_BUFFER || isCompleteTable(filtered)) {
                // Tabloyu işle ve gönder
                String tableContent = processAndReformatTable(tableBuffer.toString());
                
                // Bufferi temizle ve tablo modu kapat
                tableBuffer.setLength(0);
                tableRowCount = 0;
                isBufferingTable = false;
                
                return tableContent;
            }
            
            // Henüz tablo tamamlanmadı, boş dön (birikim için)
            return "";
        }
        
        // Normal içerik, olduğu gibi döndür
        return filtered;
    }
    
    /**
     * Tablo başlangıcını tespit et
     */
    private boolean detectTableStart(String content) {
        return content.contains("| Özellik") ||
               content.contains("|Özellik") ||
               content.startsWith("| ") && content.length() < 20 ||
               content.equals("|") ||
               content.matches("\\|\\s*\\w+.*");
    }
    
    /**
     * İçeriğin tam bir tablo olup olmadığını kontrol et
     */
    private boolean isCompleteTable(String content) {
        // Satır sayısını kontrol et
        int lineCount = content.split("\n").length;
        
        // En az 3 satır (başlık, ayraç, veri) ve sonunda boş satır varsa tam bir tablodur
        return lineCount >= 3 && content.endsWith("\n\n");
    }
    
    /**
     * Tablo içeriğini işleyerek yeniden formatlar
     */
    private String processAndReformatTable(String tableContent) {
        if (tableContent == null || tableContent.isEmpty()) {
            return tableContent;
        }
        
        try {
            // Markdown biçimlendirme sorunlarını temizle
            tableContent = tableContent.replace("**", "");
            
            // Satırları temizle ve normalize et
            String[] lines = tableContent.split("\n");
            StringBuilder result = new StringBuilder();
            
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].trim();
                
                // Boş satırları atla
                if (line.isEmpty()) {
                    continue;
                }
                
                // Tablo satırı değilse, olduğu gibi ekle
                if (!line.contains("|")) {
                    result.append(line).append("\n");
                    continue;
                }
                
                // Tablo hücrelerinin başında ve sonunda boşlukları normalize et
                line = line.replaceAll("\\|\\s+", "| ").replaceAll("\\s+\\|", " |");
                
                // Eksik olan | karakterlerini tamamla (başta veya sonda yoksa)
                if (!line.startsWith("|")) {
                    line = "| " + line;
                }
                if (!line.endsWith("|")) {
                    line = line + " |";
                }
                
                // Yeniden formatlanmış satırı ekle
                result.append(line).append("\n");
                
                // Başlık satırından sonra ayraç satırı oluştur (yoksa)
                if (i == 0 && (lines.length < 2 || !lines[1].contains("|-"))) {
                    int pipeCount = (int) line.chars().filter(c -> c == '|').count();
                    StringBuilder separator = new StringBuilder("|");
                    for (int j = 1; j < pipeCount; j++) {
                        separator.append(" --- |");
                    }
                    result.append(separator).append("\n");
                }
            }
            
            return result.toString();
        } catch (Exception e) {
            log.warn("Tablo işlenirken hata: {}", e.getMessage());
            // Hata durumunda orijinal içeriği döndür
            return tableContent;
        }
    }

    // Stream tamamlandığında biriken tablo içeriğini gönder
    // Bu yöntemi streamChatCompletion metodunda doOnComplete içinde çağırın
    private void flushTableBuffer(FluxSink<StreamResponse> sink) {
        if (isBufferingTable && tableBuffer.length() > 0) {
            String tableContent = processAndReformatTable(tableBuffer.toString());
            
            // Bufferı temizle
            tableBuffer.setLength(0);
            tableRowCount = 0;
            isBufferingTable = false;
            
            // İçeriği gönder
            if (!tableContent.isEmpty()) {
                sink.next(StreamResponse.builder()
                    .content(tableContent)
                    .done(false)
                    .build());
            }
        }
    }

    // HTML içeriğinden hata mesajını çıkaran yardımcı metod
    private String extractErrorFromHtml(String htmlContent) {
        if (htmlContent == null || htmlContent.isEmpty()) {
            return "Boş HTML yanıtı alındı";
        }
        // Model kullanılamıyor hatası için kontrol
        if (htmlContent.contains("The model") && htmlContent.contains("is not available")) {
            int startIndex = htmlContent.indexOf("The model");
            if (startIndex >= 0) {
                int endIndex = htmlContent.indexOf("</", startIndex);
                if (endIndex > startIndex) {
                    return htmlContent.substring(startIndex, endIndex)
                        .replaceAll("<[^>]*>", "")  // HTML etiketlerini kaldır
                        .trim();
                }
            }
            return "Model kullanılamıyor";
        }
        // Title içinden hata mesajını çıkar
        int titleStart = htmlContent.indexOf("<title>");
        if (titleStart >= 0) {
            int titleEnd = htmlContent.indexOf("</title>", titleStart);
            if (titleEnd > titleStart) {
                String title = htmlContent.substring(titleStart + 7, titleEnd).trim();
                if (!title.isEmpty() && !title.equalsIgnoreCase("OpenRouter")) {
                    return title;
                }
            }
        }
        // H1 içinden hata mesajını çıkar
        int h1Start = htmlContent.indexOf("<h1");
        if (h1Start >= 0) {
            int h1ContentStart = htmlContent.indexOf(">", h1Start) + 1;
            int h1End = htmlContent.indexOf("</h1>", h1ContentStart);
            if (h1End > h1ContentStart) {
                String h1Content = htmlContent.substring(h1ContentStart, h1End).trim();
                if (!h1Content.isEmpty()) {
                    return h1Content.replaceAll("<[^>]*>", "").trim();
                }
            }
        }
        // Genel hata mesajı
        return "HTML yanıtı alındı. Model kullanılamıyor veya servis hatası mevcut.";
    }

    // API bağlantı durumunu kontrol eden yeni metot
    private Mono<Boolean> checkApiConnection() {
        return openRouterWebClient.get()
            .uri("/models")  // OpenRouter'ın sağlık kontrolü için model listesini kullan
            .retrieve()
            .bodyToMono(String.class)
            .map(response -> true)  // herhangi bir yanıt alınırsa, API erişilebilir demektir
            .onErrorResume(e -> {
                log.error("API bağlantı kontrolü başarısız: {}", e.getMessage());
                return Mono.just(false);  // hata durumunda API erişilemez kabul et
            })
            .timeout(Duration.ofSeconds(5), Mono.just(false));  // 5 saniye yanıt gelmezse bağlantı yok kabul et
    }

    public Mono<AIResponse> enhancePrompt(AIRequest request) {
        // Varsayılan değerler için sıcaklık ve maksimum token değerlerini ayarla
        if (request.getTemperature() == null) {
            request.setTemperature(0.3); // Daha kararlı sonuçlar için düşük sıcaklık
        }
        
        // Token limitini ayarla
        if (request.getMaxTokens() == null) {
            request.setMaxTokens(2000); // Prompt iyileştirme için daha küçük token limiti yeterli
        }
        
        if (request.getRequestId() == null) {
            request.setRequestId(UUID.randomUUID().toString());
        }
        
        // Sistem promptunu hazırla
        String systemPrompt = getPromptEnhancementSystemPrompt(request.getLanguage());
        
        // Mesajları oluştur
        List<Map<String, Object>> messages = new ArrayList<>();
        
        // Sistem mesajını ekle
        Map<String, Object> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);
        messages.add(systemMessage);
        
        // Kullanıcı mesajını ekle
        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", request.getPrompt());
        messages.add(userMessage);
        
        // İstek özelliklerini ayarla
        request.setMessages(messages);
        request.setModel("google/gemini-2.0-flash-lite-001"); // Daha hızlı ve güncel model kullanımı
        
        // API isteği body'sini hazırla
        Map<String, Object> requestBody = createRequestBody(request);
        log.debug("Prompt iyileştirme isteği: {}", requestBody);
        
        return openRouterWebClient.post()
            .uri("/chat/completions")
            .bodyValue(requestBody)
            .headers(headers -> {
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("Accept", "application/json");
            })
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, response -> 
                response.bodyToMono(String.class)
                    .flatMap(error -> Mono.error(new APIException("Client error: " + error))))
            .onStatus(HttpStatusCode::is5xxServerError, response -> 
                response.bodyToMono(String.class)
                    .flatMap(error -> Mono.error(new APIException("Server error: " + error))))
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
            .doOnNext(response -> log.debug("Prompt iyileştirme yanıtı alındı: {}", response))
            .map(response -> {
                // Yanıt metnini çıkar
                String responseText = extractResponseText(response);
                return AIResponse.builder()
                    .response(responseText)
                    .requestId(request.getRequestId())
                    .success(true)
                    .build();
            })
            .timeout(Duration.ofSeconds(30))
            .doOnError(e -> log.error("Prompt iyileştirme hatası: {}", e.getMessage(), e))
            .onErrorResume(e -> {
                log.error("Prompt iyileştirme hatası yakalandı: {}", e.getMessage());
                return Mono.just(AIResponse.error("Prompt iyileştirilemedi: " + e.getMessage()));
            });
    }

    private String getPromptEnhancementSystemPrompt(String language) {
        // İngilizce sistem promptu
        String englishSystemPrompt = "You are an expert prompt engineer. Your task is to transform the user's text into a more effective prompt that will be directed to an AI chat model.\n\n" +
            "Follow these steps:\n" +
            "1. Create a clearer and more detailed expression\n" +
            "2. Clarify context and objectives\n" +
            "3. Remove unnecessary words\n" +
            "4. Use more specific and descriptive language\n" +
            "5. Emphasize important details\n" +
            "6. Structure and organize the prompt well\n" +
            "7. Specify the response format if necessary\n\n" +
            "Important rules:\n" +
            "- Preserve the user's original language\n" +
            "- Only create a better prompt, don't do anything else\n" +
            "- If small changes are sufficient, don't modify the original too much\n" +
            "- Don't add additional explanations, ONLY return the improved prompt text\n\n" +
            "User text to improve:\n" +
            "\"{prompt}\"";
        // Türkçe sistem promptu
        String turkishSystemPrompt = "Sen uzman bir prompt mühendisisin. Kullanıcının verdiği metni bir AI sohbet modeline yöneltilecek daha etkili bir prompt'a dönüştürmekle görevlisin.\n\n" +
            "Aşağıdaki adımları izle:\n" +
            "1. Daha net ve detaylı bir ifade oluştur\n" +
            "2. Bağlam ve hedefleri netleştir\n" +
            "3. Gereksiz kelimelerden arındır\n" +
            "4. Daha spesifik ve açıklayıcı bir dil kullan\n" +
            "5. Önemli detayları vurgula\n" +
            "6. Yapılandırılmış ve iyi organize edilmiş bir prompt format\n" +
            "7. Gerektiğinde yanıt formatını belirt\n\n" +
            "Önemli kurallar:\n" +
            "- Kullanıcının orijinal dilini koru\n" +
            "- Sadece daha iyi bir prompt oluştur, farklı bir şey yapma\n" +
            "- Küçük değişiklikler yeterli ise orijinali fazla değiştirme\n" +
            "- Ek açıklamalar ekleme, SADECE iyileştirilmiş prompt metnini döndür";
        // Dil tercihine göre uygun sistem promptunu döndür
        if (language != null && language.equalsIgnoreCase("tr")) {
            return turkishSystemPrompt;
        }
        return englishSystemPrompt;
    }

    private Mono<Map<String, Object>> callOpenRouter(String endpoint, AIRequest request) {
        Map<String, Object> requestBody = createRequestBody(request);
        // Endpoint normalizasyonu
        if (endpoint.startsWith("/")) {
            endpoint = endpoint.substring(1);  // Başlangıçtaki slash'ı kaldır
        }
        // Duplicate /api/v1 önlemek için kontrol
        String uri;
        if (endpoint.startsWith("api/v1") || endpoint.startsWith("api/v1/")) {
            uri = "/" + endpoint;  // Endpoint zaten api/v1 içeriyor
        } else {
            uri = "/api/v1/" + endpoint;  // api/v1 prefixi ekle
        }
        log.debug("OpenRouter isteği: {} - Body: {}", uri, requestBody);
        return openRouterWebClient.post()
            .uri(uri)  // Düzeltilmiş endpoint
            .bodyValue(requestBody)
            .headers(headers -> {
                headers.setContentType(MediaType.APPLICATION_JSON);
                // Birden fazla kabul edilebilir içerik türü belirtiyoruz
                headers.set("Accept", "application/json, text/plain, text/html, */*");
            })
            .exchangeToMono(response -> {
                if (response.statusCode().is2xxSuccessful()) {
                    // İçerik türünü kontrol et
                    MediaType contentType = response.headers().contentType().orElse(MediaType.APPLICATION_JSON);
                    if (contentType.includes(MediaType.APPLICATION_JSON)) {
                        // JSON yanıtı - normal işleme
                        return response.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
                    } else if (contentType.includes(MediaType.TEXT_HTML) || 
                               contentType.includes(MediaType.TEXT_PLAIN)) {
                        // HTML veya düz metin yanıtı - metni al ve bir hata mesajı oluştur
                        return response.bodyToMono(String.class)
                                .flatMap(htmlContent -> {
                                    log.error("HTML yanıtı alındı: {} karakterlik içerik", 
                                        htmlContent != null ? htmlContent.length() : 0);
                                    Map<String, Object> errorMap = new HashMap<>();
                                    errorMap.put("error", "API HTML yanıtı döndü. Servis geçici olarak kullanılamıyor olabilir.");
                                    return Mono.just(errorMap);
                                });
                    } else {
                        // Diğer içerik türleri - bilgi veren bir hata yanıtı döndür
                        log.warn("Beklenmeyen içerik türü: {}", contentType);
                        Map<String, Object> errorMap = new HashMap<>();
                        errorMap.put("error", "Beklenmeyen içerik türü: " + contentType);
                        return Mono.just(errorMap);
                    }
                } else {
                    // Hata durumları için
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
            })
            .timeout(Duration.ofSeconds(30))
            .doOnError(e -> log.error("OpenRouter API isteği sırasında hata: {}", e.getMessage(), e));
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
        List<Map<String, Object>> messages;
        // Eğer messages dizisi mevcutsa, onu kullan
        if (request.getMessages() != null && !request.getMessages().isEmpty()) {
            messages = new ArrayList<>(request.getMessages());
            // Sistem mesajı var mı kontrol et
            boolean hasSystemMessage = messages.stream()
                .anyMatch(msg -> "system".equals(msg.get("role")));
            // Yoksa ekle
            if (!hasSystemMessage) {
                Map<String, Object> systemMessage = new HashMap<>();
                systemMessage.put("role", "system");
                systemMessage.put("content", getSystemPrompt(request.getRequestType(), request.getLanguage()));
                // Sistem mesajını listenin başına ekle
                messages.add(0, systemMessage);
            }
        }
        // Değilse, prompt alanından messages oluştur (geriye dönük uyumluluk)
        else if (request.getPrompt() != null && !request.getPrompt().isEmpty()) {
            messages = new ArrayList<>();
            // Sistem mesajını ekle
            Map<String, Object> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", getSystemPrompt(request.getRequestType(), request.getLanguage()));
            messages.add(systemMessage);
            // Kullanıcı mesajını ekle
            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", request.getPrompt());
            messages.add(userMessage);
        } else {
            // Her iki alan da boşsa, hata fırlat
            throw new IllegalArgumentException("Request must contain either 'prompt' or 'messages'");
        }
        body.put("messages", messages);
        body.put("max_tokens", request.getMaxTokens());
        body.put("temperature", request.getTemperature());
        log.debug("Oluşturulan request body: {}", body);
        return body;
    }

    private String getSystemPrompt(String requestType, String language) {
        String basePrompt;
        if ("CODE".equalsIgnoreCase(requestType)) {
            basePrompt = "You are an expert coding assistant. Provide clean, efficient, and well-documented code solutions. " +
                   "Explain your approach briefly when helpful, focusing on best practices and performance considerations. " +
                   "When providing code, ensure it's production-ready and includes appropriate error handling.";
        } else if ("CHAT".equalsIgnoreCase(requestType)) {
            basePrompt = "You are a helpful, accurate, and thoughtful assistant. Provide clear, concise, and relevant responses. " +
                   "Maintain context throughout the conversation and ask clarifying questions when necessary. " +
                   "Balance thoroughness with brevity based on the user's needs. " +
                   "Always aim to provide factually correct information and acknowledge limitations in your knowledge.";
        } else {
            basePrompt = "You are a helpful AI assistant. Provide accurate, relevant, and detailed responses to the user's requests.";
        }
        // Kullanıcının diline göre yanıt vermesi için ek talimatlar ekle
        if (language != null && !language.equalsIgnoreCase("en")) {
            String languageName = getLanguageName(language);
            return basePrompt + " Always respond in " + languageName + " language unless explicitly asked to use a different language.";
        }
        return basePrompt;
    }

    private String getLanguageName(String languageCode) {
        Map<String, String> languageMap = buildSystemPrompts();
        return languageMap.getOrDefault(languageCode.toLowerCase(), "the user's preferred language (" + languageCode + ")");
    }

    private Map<String, String> buildSystemPrompts() {
        Map<String, String> prompts = new HashMap<>();
        // Ana promptları ekle
        prompts.put("DEFAULT", "You are a helpful AI assistant.");
        prompts.put("DEFAULT_TR", "Yardımsever bir yapay zeka asistanısın.");
        prompts.put("CODE", "You are an expert software developer. Write clean, efficient, and well-dokümante edilmiş kod yaz.");
        prompts.put("CODE_TR", "Uzman bir yazılım geliştiricisisin. Temiz, verimli ve iyi dokümante edilmiş kod yaz.");
        prompts.put("CHAT", "You are a friendly conversational AI. Be helpful and engaging.");
        prompts.put("CHAT_TR", "Arkadaş canlısı bir sohbet yapay zekasısın. Yardımsever ve ilgi çekici ol.");
        prompts.put("EXPLAIN", "You are a skilled teacher. Explain concepts clearly and thoroughly.");
        prompts.put("EXPLAIN_TR", "Yetenekli bir öğretmensin. Kavramları net ve detaylı açıkla.");
        prompts.put("ANALYZE", "You are an analytical expert. Provide detailed analysis and insights.");
        prompts.put("ANALYZE_TR", "Analitik bir uzmanısın. Detaylı analiz ve içgörüler sun.");
        prompts.put("BRAINSTORM", "You are a creative thinker. Generate innovative ideas and solutions.");
        prompts.put("BRAINSTORM_TR", "Yaratıcı bir düşünürsün. Yenilikçi fikirler ve çözümler üret.");
        prompts.put("DATA", "You are a data analyst. Process and explain data patterns effectively.");
        prompts.put("DATA_TR", "Bir veri analistisin. Veri desenlerini etkili şekilde işle ve açıkla.");
        prompts.put("REVIEW", "You are a thorough reviewer. Provide constructive and detailed feedback.");
        prompts.put("REVIEW_TR", "Detaylı bir eleştirmensin. Yapıcı ve ayrıntılı geri bildirim ver.");
        prompts.put("PLAN", "You are a strategic planner. Create organized and actionable plans.");
        prompts.put("PLAN_TR", "Stratejik bir planlayıcısın. Organize ve uygulanabilir planlar oluştur.");
        prompts.put("SUMMARIZE", "You are an efficient summarizer. Extract and convey key information concisely.");
        prompts.put("SUMMARIZE_TR", "Verimli bir özetleyicisin. Önemli bilgileri özlü bir şekilde çıkar ve ilet.");
        prompts.put("DEBUG", "You are a skilled debugger. Help identify and fix technical issues.");
        prompts.put("DEBUG_TR", "Yetenekli bir hata ayıklayıcısın. Teknik sorunları belirleme ve çözme konusunda yardımcı ol.");
        return Map.copyOf(prompts);  // Değiştirilemez kopya dön
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
        if (response == null) {
            log.warn("extractResponseText: Yanıt null");
            return null;
        }
        
        log.debug("extractResponseText işleniyor: {}", response);
        
        try {
            // choices[0].message.content alanını kontrol et
            if (response.containsKey("choices") && response.get("choices") instanceof List) {
                List<?> choices = (List<?>) response.get("choices");
                if (!choices.isEmpty() && choices.get(0) instanceof Map) {
                    Map<?, ?> choice = (Map<?, ?>) choices.get(0);
                    if (choice.containsKey("message") && choice.get("message") instanceof Map) {
                        Map<?, ?> message = (Map<?, ?>) choice.get("message");
                        if (message.containsKey("content")) {
                            String content = String.valueOf(message.get("content"));
                            log.debug("Content bulundu: {}", content.length() > 100 ? content.substring(0, 100) + "..." : content);
                            return content;
                        }
                    }
                    
                    // Alternatif: doğrudan "text" alanı var mı kontrol et
                    if (choice.containsKey("text")) {
                        String text = String.valueOf(choice.get("text"));
                        log.debug("Text bulundu: {}", text.length() > 100 ? text.substring(0, 100) + "..." : text);
                        return text;
                    }
                }
            }
            
            // En üst seviyede content var mı kontrol et
            if (response.containsKey("content")) {
                String content = String.valueOf(response.get("content"));
                log.debug("Üst seviye content bulundu: {}", content.length() > 100 ? content.substring(0, 100) + "..." : content);
                return content;
            }
            
            // Yanıtı okunamadığında JSON'ı string olarak döndür (tanılama için)
            log.warn("Yanıttan mesaj içeriği çıkarılamadı, yanıt yapısı: {}", response.keySet());
            return "Yanıt içeriği okunamadı. Teknik detay: " + response.keySet();
            
        } catch (Exception e) {
            log.error("Yanıt metni çıkarılırken hata: {}", e.getMessage(), e);
            return "Yanıt işlenemedi: " + e.getMessage();
        }
    }

    private Integer extractTokenCount(Map<String, Object> response) {
        @SuppressWarnings("unchecked")
        Map<String, Object> usage = (Map<String, Object>) response.getOrDefault("usage", Map.of());
        return ((Number) usage.getOrDefault("total_tokens", 0)).intValue();
    }

    // Model adından sağlayıcı adını çıkarır
    private boolean isStreamingSupported(String model) {
        if (model == null || model.isEmpty()) {
            return true; // Varsayılan olarak destekleniyor kabul et
        }
        String provider = model.split("/")[0].toLowerCase();
        return true;
    }
}