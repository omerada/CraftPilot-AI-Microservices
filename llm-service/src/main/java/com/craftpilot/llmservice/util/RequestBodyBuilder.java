package com.craftpilot.llmservice.util;

import com.craftpilot.llmservice.config.OpenRouterProperties;
import com.craftpilot.llmservice.model.AIRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class RequestBodyBuilder {

    private final OpenRouterProperties properties;

    public RequestBodyBuilder(OpenRouterProperties properties) {
        this.properties = properties;
        log.info("RequestBodyBuilder initialized with properties: {}", properties);
    }

    public Map<String, Object> createRequestBody(AIRequest request) {
        Map<String, Object> requestBody = new HashMap<>();

        // Mevcut uygulamanın mantığına göre request body'yi oluştur
        if (request != null) {
            // Model bilgisini ayarla
            if (request.getModel() != null) {
                requestBody.put("model", request.getModel());
            }

            // Mesajları ayarla
            if (request.getMessages() != null) {
                requestBody.put("messages", request.getMessages());
            }

            // Temperature ayarla (varsayılan: 0.7)
            requestBody.put("temperature", request.getTemperature() != null ? request.getTemperature() : 0.7);

            // Diğer parametreleri ekle
            if (request.getMaxTokens() != null) {
                requestBody.put("max_tokens", request.getMaxTokens());
            }

            if (request.getTopP() != null) {
                requestBody.put("top_p", request.getTopP());
            }

            if (request.getFrequencyPenalty() != null) {
                requestBody.put("frequency_penalty", request.getFrequencyPenalty());
            }

            if (request.getPresencePenalty() != null) {
                requestBody.put("presence_penalty", request.getPresencePenalty());
            }

            // Stream özelliğini ayarla
            if (request.isStream() != null) {
                requestBody.put("stream", request.isStream());
            }
        }

        return requestBody;
    }
}
