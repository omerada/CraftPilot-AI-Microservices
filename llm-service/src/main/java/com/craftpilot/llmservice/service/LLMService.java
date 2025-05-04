package com.craftpilot.llmservice.service;

import com.craftpilot.llmservice.model.AIRequest;
import com.craftpilot.llmservice.model.AIResponse;
import com.craftpilot.llmservice.model.StreamResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * LLM servislerini bir araya getiren facade servisi
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LLMService {
    private final ChatCompletionService chatCompletionService;
    private final PromptEnhancementService promptEnhancementService;
    private final StreamingService streamingService;

    /**
     * Chat tamamlama isteğini işler
     */
    public Mono<AIResponse> processChatCompletion(AIRequest request) {
        return chatCompletionService.processChatCompletion(request);
    }

    /**
     * Kod tamamlama isteğini işler
     */
    public Mono<AIResponse> processCodeCompletion(AIRequest request) {
        return chatCompletionService.processCodeCompletion(request);
    }

    /**
     * Görsel oluşturma isteğini işler (şu an desteklenmiyor)
     */
    public Mono<AIResponse> processImageGeneration(AIRequest request) {
        return Mono.error(new UnsupportedOperationException(
            "Image generation is not supported by OpenRouter. Please use Stability AI or DALL-E API directly."));
    }

    /**
     * Chat tamamlama isteğini stream olarak işler
     */
    public Flux<StreamResponse> streamChatCompletion(AIRequest request) {
        return streamingService.streamChatCompletion(request);
    }

    /**
     * Prompt iyileştirme isteğini işler
     */
    public Mono<AIResponse> enhancePrompt(AIRequest request) {
        return promptEnhancementService.enhancePrompt(request);
    }
}