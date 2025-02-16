package com.craftpilot.llmservice.controller;

import com.craftpilot.llmservice.model.AIRequest;
import com.craftpilot.llmservice.model.AIResponse;
import com.craftpilot.llmservice.service.OpenRouterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Tag(name = "LLM API", description = "AI dil modeli işlemleri için endpoints")
public class LLMController {
    private final OpenRouterService openRouterService;

    @PostMapping("/chat")
    @Operation(summary = "Sohbet yanıtı üret", description = "Verilen mesajlar için AI yanıtı üretir")
    public Mono<AIResponse> chat(@RequestBody AIRequest request) {
        log.info("Chat request received for model: {}", request.getModel());
        return openRouterService.processRequest(request);
    }

    @PostMapping("/translate")
    @Operation(summary = "Metin çeviri", description = "Verilen metni hedef dile çevirir")
    public Mono<AIResponse> translate(@RequestBody AIRequest request) {
        log.info("Translation request received for model: {}", request.getModel());
        request.setRequestType("translation");
        return openRouterService.processRequest(request);
    }

    @PostMapping("/code")
    @Operation(summary = "Kod üret/düzenle", description = "Verilen prompt'a göre kod üretir veya düzenler")
    public Mono<AIResponse> generateCode(@RequestBody AIRequest request) {
        log.info("Code generation request received for model: {}", request.getModel());
        request.setRequestType("code");
        return openRouterService.processRequest(request);
    }

    @PostMapping("/content")
    @Operation(summary = "İçerik üret", description = "Verilen prompt'a göre içerik üretir")
    public Mono<AIResponse> generateContent(@RequestBody AIRequest request) {
        log.info("Content generation request received for model: {}", request.getModel());
        request.setRequestType("content");
        return openRouterService.processRequest(request);
    }
}