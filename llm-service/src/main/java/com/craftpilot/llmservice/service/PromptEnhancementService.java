package com.craftpilot.llmservice.service;

import com.craftpilot.llmservice.config.OpenRouterProperties;
import com.craftpilot.llmservice.exception.APIException;
import com.craftpilot.llmservice.model.AIRequest;
import com.craftpilot.llmservice.model.AIResponse;
import com.craftpilot.llmservice.service.client.OpenRouterClient;
import com.craftpilot.llmservice.util.ResponseExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Prompt iyileştirme işlemlerini yönetir
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PromptEnhancementService {
    private final OpenRouterClient openRouterClient;
    private final ResponseExtractor responseExtractor;
    private final OpenRouterProperties properties;

    /**
     * Verilen AI isteğindeki promptu iyileştirir
     */
    public Mono<AIResponse> enhancePrompt(AIRequest request) {
        // Varsayılan değerler için sıcaklık ve maksimum token değerlerini ayarla
        if (request.getTemperature() != null && request.getTemperature() == 0.0) {
            request.setTemperature(0.3); // Daha kararlı sonuçlar için düşük sıcaklık
        }
        
        // Token limitini ayarla
        if (request.getMaxTokens() != null && request.getMaxTokens() == 0) {
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
        
        log.debug("Prompt iyileştirme isteği oluşturuldu: {}", request);
        
        return openRouterClient.callOpenRouter("chat/completions", request)
            .doOnNext(response -> log.debug("Prompt iyileştirme yanıtı alındı: {}", response))
            .map(response -> {
                // Yanıt metnini çıkar
                String responseText = responseExtractor.extractResponseText(response);
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
                AIResponse errorResponse = AIResponse.builder()
                    .error("Error enhancing prompt")
                    .success(false)
                    .build();
                return Mono.just(errorResponse);
            });
    }
    
    /**
     * Belirtilen dile göre prompt iyileştirme sistem promptunu döndürür
     */
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
}
