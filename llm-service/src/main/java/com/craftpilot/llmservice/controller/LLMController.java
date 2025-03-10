package com.craftpilot.llmservice.controller;

import com.craftpilot.llmservice.model.AIRequest;
import com.craftpilot.llmservice.model.AIResponse;
import com.craftpilot.llmservice.model.StreamResponse;
import com.craftpilot.llmservice.service.LLMService;
import com.craftpilot.llmservice.exception.ValidationException;
import com.craftpilot.llmservice.exception.APIException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/")  
@RequiredArgsConstructor 
public class LLMController {
    
    private final LLMService llmService;

    @PostMapping(value = "/chat/completions", 
                produces = MediaType.APPLICATION_JSON_VALUE,
                consumes = MediaType.APPLICATION_JSON_VALUE) 
    public Mono<ResponseEntity<AIResponse>> chatCompletion(@RequestBody AIRequest request,
                                                         @RequestHeader(value = "X-User-Language", defaultValue = "en") String userLanguage) {
        log.info("Chat completion request received with language: {}", userLanguage);
        request.setRequestType("CHAT");
        request.setLanguage(userLanguage); // Kullanıcı dil tercihini request'e ekle
        
        return llmService.processChatCompletion(request)
            .doOnSuccess(response -> log.debug("Chat completion success"))
            .map(response -> ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response))
            .doOnError(error -> {
                log.error("Chat completion error: {}", error.getMessage(), error);
                // Detaylı hata logu
                if (error.getCause() != null) {
                    log.error("Root cause: {}", error.getCause().getMessage());
                }
            })
            .onErrorResume(error -> {
                String errorMessage = error.getMessage();
                if (errorMessage != null && errorMessage.contains("API hatası:")) {
                    // API hatasını doğrudan ilet
                    return Mono.just(ResponseEntity
                        .status(HttpStatus.BAD_GATEWAY)
                        .body(AIResponse.error(errorMessage)));
                } else {
                    // Genel hata mesajı
                    return Mono.just(ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(AIResponse.error("LLM servisinde bir hata oluştu: " + errorMessage)));
                }
            });
    }

    @PostMapping(value = "/chat/completions/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE) 
    public Flux<StreamResponse> streamChatCompletion(@RequestBody AIRequest request,
                                                  @RequestHeader(value = "X-User-Language", defaultValue = "en") String userLanguage) {
        log.info("Stream chat completion request received with language: {}", userLanguage);
        request.setRequestType("CHAT");
        request.setLanguage(userLanguage);
        
        return llmService.streamChatCompletion(request)
            // Yanıtları hemen istemciye gönder - immediate scheduler ile
            .publishOn(Schedulers.immediate())
            // Backpressure stratejisi - DROP ile aşırı yük durumunda bazı yanıtları atlayabilir
            // ama gecikme olmaz
            .onBackpressureDrop(item -> log.warn("Dropped stream item due to backpressure"))
            .doOnNext(response -> {
                if (log.isDebugEnabled()) {
                    log.debug("Sending chunk immediately: {}", 
                        response.getContent().length() > 20 
                            ? response.getContent().substring(0, 20) + "..." 
                            : response.getContent());
                }
            })
            .doOnError(error -> log.error("Stream error: {}", error.getMessage(), error))
            .doOnComplete(() -> log.info("Stream completed for request"));
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