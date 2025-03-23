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
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import jakarta.validation.Valid;
import java.util.UUID;

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
    public Flux<ServerSentEvent<StreamResponse>> streamChatCompletion(
            @RequestBody AIRequest request,
            @RequestHeader(value = "X-User-Language", defaultValue = "en") String userLanguage,
            @RequestHeader(value = "X-Request-ID", required = false) String requestId,
            ServerWebExchange exchange) {
        
        log.info("Stream chat completion request received with language: {}, requestId: {}", 
                userLanguage, requestId != null ? requestId : "not provided");
        
        request.setRequestType("CHAT");
        request.setLanguage(userLanguage);
        
        // İstemci bağlantıyı keserse işlemi iptal etmek için
        Mono<Void> cancelSignal = exchange.getResponse().setComplete()
                .then(Mono.fromRunnable(() -> 
                    log.info("Client disconnected, cancelling stream for request: {}", requestId)));
        
        return llmService.streamChatCompletion(request)
            // Yanıtları hemen istemciye gönder - immediate scheduler ile
            .publishOn(Schedulers.immediate())
            // Akışı SSE formatına dönüştür
            .map(chunk -> ServerSentEvent.<StreamResponse>builder()
                    .id(requestId != null ? requestId : UUID.randomUUID().toString())
                    .event("message")
                    .data(chunk)
                    .build())
            // İstemci bağlantıyı keserse akışı iptal et
            .takeUntilOther(cancelSignal)
            // Backpressure stratejisi - DROP ile aşırı yük durumunda bazı yanıtları atlayabilir
            .onBackpressureDrop(item -> log.warn("Dropped stream item due to backpressure"))
            .doOnNext(response -> {
                if (log.isDebugEnabled()) {
                    String content = response.data().getContent();
                    log.debug("Sending chunk: {}", 
                        content != null && content.length() > 20 
                            ? content.substring(0, 20) + "..." 
                            : content);
                }
            })
            // OpenRouter processing mesajı için özel SSE yorumu ekle
            .startWith(ServerSentEvent.<StreamResponse>builder()
                    .comment("OPENROUTER PROCESSING")
                    .build())
            .doOnError(error -> log.error("Stream error: {}", error.getMessage(), error))
            .doOnComplete(() -> log.info("Stream completed for request: {}", requestId))
            .onErrorResume(error -> {
                log.error("Stream error occurred: {}", error.getMessage(), error);
                return Flux.just(ServerSentEvent.<StreamResponse>builder()
                        .event("error")
                        .data(StreamResponse.builder()
                                .content("Hata: " + error.getMessage())
                                .done(true)
                                .build())
                        .build());
            });
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

    @PostMapping(value = "/enhance-prompt", 
                produces = MediaType.APPLICATION_JSON_VALUE,
                consumes = MediaType.APPLICATION_JSON_VALUE) 
    public Mono<ResponseEntity<AIResponse>> enhancePrompt(@RequestBody AIRequest request,
                                                        @RequestHeader(value = "X-User-Language", defaultValue = "en") String userLanguage) {
        log.info("Prompt enhancement request received with language: {}", userLanguage);
        request.setRequestType("ENHANCE");
        request.setLanguage(userLanguage);
        
        return llmService.enhancePrompt(request)
            .doOnSuccess(response -> log.debug("Prompt enhancement success"))
            .map(response -> ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response))
            .doOnError(error -> {
                log.error("Prompt enhancement error: {}", error.getMessage(), error);
                if (error.getCause() != null) {
                    log.error("Root cause: {}", error.getCause().getMessage());
                }
            })
            .onErrorResume(error -> {
                String errorMessage = error.getMessage();
                if (errorMessage != null && errorMessage.contains("API hatası:")) {
                    return Mono.just(ResponseEntity
                        .status(HttpStatus.BAD_GATEWAY)
                        .body(AIResponse.error("Prompt iyileştirilemedi: " + errorMessage)));
                } else {
                    return Mono.just(ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(AIResponse.error("Prompt iyileştirilemedi: " + errorMessage)));
                }
            });
    }
}