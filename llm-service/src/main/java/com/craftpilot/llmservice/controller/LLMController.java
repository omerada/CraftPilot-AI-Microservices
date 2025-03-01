package com.craftpilot.llmservice.controller;

import com.craftpilot.llmservice.model.AIRequest;
import com.craftpilot.llmservice.model.AIResponse;
import com.craftpilot.llmservice.service.LLMService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Tag(name = "LLM API")
public class LLMController {
    private final LLMService llmService;
    
    @PostMapping("/completions")
    public Mono<ResponseEntity<AIResponse>> textCompletion(@Valid @RequestBody AIRequest request) {
        return llmService.processTextCompletion(request)
            .map(ResponseEntity::ok)
            .doOnError(e -> log.error("Error processing completion", e))
            .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(AIResponse.error(e.getMessage()))));
    }

    @PostMapping("/chat/completions")
    public Mono<ResponseEntity<AIResponse>> chatCompletion(@RequestBody AIRequest request) {
        return llmService.processChatCompletion(request)
            .map(response -> ResponseEntity.ok().body(response))
            .doOnError(error -> log.error("Chat completion error: ", error));
    }

    @PostMapping("/code/completion")
    public Mono<ResponseEntity<AIResponse>> codeCompletion(@RequestBody AIRequest request) {
        return llmService.processCodeCompletion(request)
            .map(response -> ResponseEntity.ok().body(response))
            .doOnError(error -> log.error("Code completion error: ", error));
    }
}