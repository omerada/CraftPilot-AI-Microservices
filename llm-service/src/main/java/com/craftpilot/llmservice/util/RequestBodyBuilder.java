package com.craftpilot.llmservice.util;

import com.craftpilot.llmservice.config.OpenRouterProperties;
import com.craftpilot.llmservice.model.AIRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API istek gövdelerini oluşturmak için yardımcı sınıf
 */
@Slf4j
@Component
public class RequestBodyBuilder {

    private final OpenRouterProperties properties;

    @Autowired
    public RequestBodyBuilder(OpenRouterProperties properties) {
        this.properties = properties;
        log.info("RequestBodyBuilder initialized with properties: {}", properties);
    }

    /**
     * AI isteğinden API istek gövdesi oluşturur
     */
    public Map<String, Object> createRequestBody(AIRequest request) {
        Map<String, Object> body = new HashMap<>();

        // Model bilgisini ekle
        body.put("model", request.getModel());

        // Mesajları hazırla
        List<Map<String, Object>> messages = prepareMessages(request);
        body.put("messages", messages);

        // Diğer parametreleri ekle
        body.put("max_tokens", request.getMaxTokens());
        body.put("temperature", request.getTemperature());

        log.debug("Oluşturulan request body: {}", body);
        return body;
    }

    /**
     * Mesajları hazırlar, sistem mesajını ve kullanıcı mesajını ayarlar
     */
    private List<Map<String, Object>> prepareMessages(AIRequest request) {
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
                systemMessage.put("content", request.getSystemPrompt() != null ? request.getSystemPrompt()
                        : properties.getDefaultSystemPrompt());

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
            systemMessage.put("content", request.getSystemPrompt() != null ? request.getSystemPrompt()
                    : properties.getDefaultSystemPrompt());
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

        return messages;
    }
}
