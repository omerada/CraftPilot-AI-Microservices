package com.craftpilot.llmservice.controller;

import com.craftpilot.llmservice.model.AIRequest;
import com.craftpilot.llmservice.model.AIResponse;
import com.craftpilot.llmservice.model.StreamResponse;
import com.craftpilot.llmservice.service.LLMService;
import com.craftpilot.llmservice.exception.ValidationException;
import com.craftpilot.llmservice.exception.APIException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor 
public class LLMController {
    
    private final LLMService llmService;
    
    @PostMapping(value = "/completions",
                produces = MediaType.APPLICATION_JSON_VALUE,
                consumes = MediaType.APPLICATION_JSON_VALUE) 
    public Mono<ResponseEntity<AIResponse>> textCompletion(
            @Valid @RequestBody AIRequest request,
            @RequestHeader(required = false) String userId) {
        
        log.info("Text completion request received: {}", request);
        
        return llmService.processTextCompletion(request)
            .doOnNext(this::auditResponse)
            .map(ResponseEntity::ok)
            .doOnError(e -> log.error("Error processing completion", e))
            .onErrorResume(this::handleError);
    }

    private Mono<ResponseEntity<AIResponse>> handleError(Throwable error) {
        log.error("Error processing request", error); // Detaylı loglama ekledik
        
        if (error instanceof ValidationException) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(AIResponse.error("Validation error: " + error.getMessage())));
        } else if (error instanceof APIException) {
            return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(AIResponse.error("AI Service error: " + error.getMessage())));
        }
        
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(AIResponse.error("Internal server error: " + error.getMessage())));
    }

    private void auditResponse(AIResponse response) {
        // Audit logging implementation
    }

    @PostMapping(value = "/chat/completions", 
                produces = MediaType.APPLICATION_JSON_VALUE,
                consumes = MediaType.APPLICATION_JSON_VALUE) 
    public Mono<ResponseEntity<AIResponse>> chatCompletion(@RequestBody AIRequest request) {
        request.setRequestType("CHAT");
        return llmService.processChatCompletion(request)
            .map(response -> ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response))
            .doOnError(error -> log.error("Chat completion error: ", error))
            .doOnSuccess(response -> log.debug("Chat completion success: {}", response));
    }

    @PostMapping(value = "/chat/completions/stream", 
                produces = MediaType.TEXT_EVENT_STREAM_VALUE) 
    public Flux<StreamResponse> streamChatCompletion(@RequestBody AIRequest request) {
        request.setRequestType("CHAT");
        return llmService.streamChatCompletion(request)
            .doOnNext(response -> log.debug("Streaming response chunk: {}", response))
            .doOnError(error -> log.error("Stream error: ", error));
    }

    @PostMapping(value = "/images/generate", 
                produces = MediaType.APPLICATION_JSON_VALUE,
                consumes = MediaType.APPLICATION_JSON_VALUE) 
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