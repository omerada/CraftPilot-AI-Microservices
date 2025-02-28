package com.craftpilot.llmservice.controller;

import com.craftpilot.llmservice.model.AIRequest;
import com.craftpilot.llmservice.model.AIResponse;
import com.craftpilot.llmservice.service.LLMService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import javax.validation.ValidationException;

@Slf4j
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Tag(name = "LLM API", description = "AI dil modeli işlemleri için endpoints")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Validated
public class LLMController {
    private final LLMService llmService;
    private final CacheManager cacheManager;

    @Cacheable(value = "completions", key = "#request.hashCode()")
    @PostMapping(value = "/completions", 
                produces = MediaType.APPLICATION_JSON_VALUE,
                consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Text completion", description = "Verilen prompt için text completion yapar")
    public Mono<ResponseEntity<AIResponse>> textCompletion(
            @Valid @RequestBody AIRequest request,
            @RequestHeader(required = false) String userId) {
        log.info("Text completion request received: {}", request);
        
        return llmService.processTextCompletion(request)
            .doOnNext(this::auditResponse)
            .map(ResponseEntity::ok)
            .doOnError(e -> log.error("Error processing completion", e))
            .onErrorResume(e -> handleError(e));
    }

    private Mono<ResponseEntity<AIResponse>> handleError(Throwable error) {
        if (error instanceof ValidationException) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(new AIResponse("Validation error: " + error.getMessage())));
        }
        // ... diğer error handling cases
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new AIResponse("Internal server error")));
    }

    private void auditResponse(AIResponse response) {
        // Audit logging implementation
    }

    @PostMapping(value = "/chat/completions", 
                produces = MediaType.APPLICATION_JSON_VALUE,
                consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Chat completion", description = "Sohbet formatında AI yanıtı üretir")
    public Mono<ResponseEntity<AIResponse>> chatCompletion(@RequestBody AIRequest request) {
        request.setRequestType("CHAT");
        return llmService.processChatCompletion(request)
            .map(response -> ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response))
            .doOnError(error -> log.error("Chat completion error: ", error))
            .doOnSuccess(response -> log.debug("Chat completion success: {}", response));
    }

    @PostMapping(value = "/images/generate", 
                produces = MediaType.APPLICATION_JSON_VALUE,
                consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Image generation", description = "Verilen prompt'a göre resim üretir")
    public Mono<ResponseEntity<AIResponse>> generateImage(@RequestBody AIRequest request) {
        request.setRequestType("IMAGE");
        return llmService.processImageGeneration(request)
            .map(response -> ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response))
            .doOnError(error -> log.error("Image generation error: ", error))
            .doOnSuccess(response -> log.debug("Image generation success: {}", response));
    }

    @PostMapping(value = "/code/completion", 
                produces = MediaType.APPLICATION_JSON_VALUE,
                consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Code completion", description = "Kod tamamlama ve geliştirme yapar")
    public Mono<ResponseEntity<AIResponse>> codeCompletion(@RequestBody AIRequest request) {
        request.setRequestType("CODE");
        return llmService.processCodeCompletion(request)
            .map(response -> ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response))
            .doOnError(error -> log.error("Code completion error: ", error))
            .doOnSuccess(response -> log.debug("Code completion success: {}", response));
    }
}