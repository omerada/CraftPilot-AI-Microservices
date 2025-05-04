package com.craftpilot.llmservice.service;

import com.craftpilot.llmservice.client.UserMemoryClient;
import com.craftpilot.llmservice.dto.ExtractedUserInfo;
import com.craftpilot.llmservice.model.AIRequest;
import com.craftpilot.llmservice.model.AIResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserInformationExtractionService {
    private final UserMemoryClient userMemoryClient;
    private final LLMService llmService;

    @Value("${ai.model.extraction:google/gemma-3-4b-it}")
    private String extractionModel;

    public Mono<ExtractedUserInfo> extractUserInfo(String userId, String message, String context) {
        String prompt = buildExtractionPrompt(message);
        
        AIRequest request = AIRequest.builder()
                .model(extractionModel)
                .prompt(prompt)
                .temperature(0.1) // Daha deterministik sonuçlar için düşük sıcaklık
                .build();

        return llmService.processChatCompletion(request)
                .flatMap(response -> parseExtractionResponse(userId, response, context, message));
    }

    public Mono<Void> processAndStoreUserInfo(String userId, String message, String context) {
        return extractUserInfo(userId, message, context)
                .flatMap(userMemoryClient::addMemoryEntry)
                .then();
    }

    private String buildExtractionPrompt(String message) {
        return "Aşağıdaki mesajdan kullanıcı hakkında kişisel bilgileri çıkar. " +
               "Eğer kişisel bilgi bulunamazsa, 'NO_INFORMATION' şeklinde yanıt ver. " +
               "Şu bilgileri çıkarmaya odaklan: isim, yaş, meslek, konum, ilgi alanları, hobiler, tercihler, aile detayları. " +
               "JSON formatında yanıt ver, sadece 'information' alanı içinde çıkarılan bilgiyi veya 'NO_INFORMATION' değerini döndür. " +
               "\n\nKullanıcı mesajı: " + message;
    }

    private Mono<ExtractedUserInfo> parseExtractionResponse(String userId, AIResponse response, String context, String originalMessage) {
        try {
            String content = response.getResponse();
            
            // Demo amaçlı basit çıkarım
            // Üretimde düzgün JSON ayrıştırma kullanılmalı
            if (content.contains("NO_INFORMATION")) {
                log.debug("Mesajdan bilgi çıkarılamadı");
                return Mono.empty();
            }

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

            return Mono.just(ExtractedUserInfo.builder()
                    .userId(userId)
                    .information(information)
                    .context("Mesajdan çıkarıldı: " + 
                             (originalMessage.length() > 30 ? 
                                 originalMessage.substring(0, 30) + "..." : 
                                 originalMessage))
                    .build());

        } catch (Exception e) {
            log.error("Çıkarım yanıtı ayrıştırılırken hata oluştu", e);
            return Mono.empty();
        }
    }
}
