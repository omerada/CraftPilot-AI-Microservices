package com.craftpilot.llmservice.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.FluxSink;

import com.craftpilot.llmservice.model.StreamResponse;
import java.util.List;
import java.util.Map;

/**
 * AI servislerinden alınan yanıtları ayıklamak için yardımcı sınıf
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ResponseExtractor {
    private final ObjectMapper objectMapper;
    
    /**
     * Yanıt metni çıkarımı için yardımcı metod
     */
    public String extractResponseText(Map<String, Object> response) {
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
    
    /**
     * Token sayısını çıkarır
     */
    public Integer extractTokenCount(Map<String, Object> response) {
        @SuppressWarnings("unchecked")
        Map<String, Object> usage = (Map<String, Object>) response.getOrDefault("usage", Map.of());
        return ((Number) usage.getOrDefault("total_tokens", 0)).intValue();
    }
    
    /**
     * HTML içeriğinden hata mesajını çıkarır
     */
    public String extractErrorFromHtml(String htmlContent) {
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
    
    /**
     * JSON verisinden içerik çıkarıp sink'e gönderir
     */
    public void extractAndSendContent(String jsonText, FluxSink<StreamResponse> sink) throws Exception {
        // JSON'ı parse et
        JsonNode jsonNode = objectMapper.readTree(jsonText);
        
        // İçeriği çıkar ve gönder
        checkAndSendStandardFormats(jsonNode, sink);
    }
    
    /**
     * Standart OpenAI ve diğer formatları kontrol edip içeriği çıkarır
     */
    public void checkAndSendStandardFormats(JsonNode jsonNode, FluxSink<StreamResponse> sink) {
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
                        log.debug("Extracted delta.content: {}", 
                            content.length() > 30 ? content.substring(0, 30) + "..." : content);
                        sink.next(StreamResponse.builder()
                            .content(content)
                            .done(false)
                            .build());
                        contentSent = true;
                    }
                }
                
                // Text format
                if (!contentSent && choice.has("text")) {
                    String content = choice.get("text").asText();
                    if (content != null && !content.isEmpty()) {
                        log.debug("Extracted text: {}", content);
                        sink.next(StreamResponse.builder()
                            .content(content)
                            .done(false)
                            .build());
                        contentSent = true;
                    }
                }
                
                // Message format
                if (!contentSent && choice.has("message") && choice.get("message").has("content")) {
                    String content = choice.get("message").get("content").asText();
                    if (content != null && !content.isEmpty()) {
                        log.debug("Extracted message.content: {}", content);
                        sink.next(StreamResponse.builder()
                            .content(content)
                            .done(false)
                            .build());
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
                    
                    if (!contentSent) {
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
}
