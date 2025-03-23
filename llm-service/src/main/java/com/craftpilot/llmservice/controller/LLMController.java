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
        
        // RequestID yoksa bir tane oluştur (loglamada null görmemek için)
        final String trackingId = requestId != null ? requestId : UUID.randomUUID().toString();
        
        log.info("Stream chat completion request received with language: {}, requestId: {}", 
                userLanguage, trackingId);
        
        request.setRequestType("CHAT");
        request.setLanguage(userLanguage);
        
        // Response header'larını ayarla
        exchange.getResponse().getHeaders().setContentType(MediaType.TEXT_EVENT_STREAM);
        exchange.getResponse().getHeaders().setCacheControl("no-cache");
        exchange.getResponse().getHeaders().setConnection("keep-alive");
        
        // Erken client bağlantı kopması tespiti için doğru tip dönüşümü
        Mono<Void> cancelSignal = Mono.fromRunnable(() -> {})
                .doOnCancel(() -> log.info("Client cancelled the stream for request: {}", trackingId))
                .then(); // then() metodu ile Mono<Object>'i Mono<Void>'e dönüştür
        
        return llmService.streamChatCompletion(request)
            // Yanıtları hemen istemciye gönder - immediate scheduler ile
            .publishOn(Schedulers.immediate())
            // İşlem iptalini (client disconnect) tespit et
            .doOnCancel(() -> log.warn("Stream was cancelled for request: {}", trackingId))
            // Akışı SSE formatına dönüştür
            .map(chunk -> {
                // Her yanıt parçası için detaylı loglama
                if (log.isDebugEnabled()) {
                    log.debug("Received chunk from service: {}", 
                        chunk.getContent() != null && chunk.getContent().length() > 50 
                            ? chunk.getContent().substring(0, 50) + "..." 
                            : chunk.getContent());
                }
                
                return ServerSentEvent.<StreamResponse>builder()
                    .id(trackingId)
                    .event("message") 
                    .data(chunk)
                    .build();
            })
            // İstemci bağlantıyı keserse erken bitir
            .takeUntilOther(cancelSignal)
            // SSE biçimi için özel boş yanıt ekle - OpenRouter SSE imzalama
            .startWith(
                // İlk mesaj olarak OpenRouter bekleme durumu belirtisi
                ServerSentEvent.<StreamResponse>builder()
                    .comment("OPENROUTER PROCESSING")
                    .id(trackingId)
                    .build(),
                // Boş bir yanıt ile bağlantının başladığını belirt
                ServerSentEvent.<StreamResponse>builder()
                    .id(trackingId)
                    .event("ping")  
                    .data(StreamResponse.builder()
                            .content("")
                            .done(false)
                            .build())
                    .build()
            )
            // Herhangi bir backpressure durumunda buffer kullan
            .onBackpressureBuffer(256, drop -> {
                log.warn("Dropped stream chunk due to backpressure: {}", drop);
            })
            .doOnError(error -> log.error("Stream error: {}", error.getMessage(), error))
            .doOnComplete(() -> log.info("Stream completed for request: {}", trackingId))
            .onErrorResume(error -> {
                log.error("Stream error occurred: {}", error.getMessage(), error);
                return Flux.just(ServerSentEvent.<StreamResponse>builder()
                        .id(trackingId)
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