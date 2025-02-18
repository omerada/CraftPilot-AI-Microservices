package com.craftpilot.llmservice.controller;

import com.craftpilot.llmservice.model.AIRequest;
import com.craftpilot.llmservice.model.AIResponse;
import com.craftpilot.llmservice.service.LLMService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Tag(name = "LLM API", description = "AI dil modeli işlemleri için endpoints")
public class LLMController {
    private final LLMService llmService;

    @PostMapping(value = "/completions")
    @Operation(summary = "Text completion", description = "Verilen prompt için text completion yapar")
    public Mono<ResponseEntity<AIResponse>> textCompletion(@RequestBody AIRequest request) {
        log.debug("Incoming request: {}", request);
        return Mono.just(request)
            .doOnNext(req -> {
                if (req.getPrompt() == null || req.getPrompt().trim().isEmpty()) {
                    throw new IllegalArgumentException("Prompt cannot be empty");
                }
            })
            .flatMap(req -> {
                req.setRequestType("TEXT");
                return llmService.processTextCompletion(req);
            })
            .map(response -> {
                log.debug("Response received: {}", response);
                return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response);
            })
            .doOnError(error -> log.error("Error processing request: ", error))
            .onErrorResume(error -> Mono.just(
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(AIResponse.builder()
                        .status("ERROR")
                        .error(error.getMessage())
                        .build())
            ));
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