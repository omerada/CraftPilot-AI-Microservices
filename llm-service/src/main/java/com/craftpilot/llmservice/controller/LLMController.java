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
import org.springframework.http.codec.ServerSentEvent;
import jakarta.validation.Valid;erver.ServerWebExchange;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.server.ServerWebExchange; jakarta.validation.Valid;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/")  uiredArgsConstructor 
@RequiredArgsConstructor 
public class LLMController {    
    
    private final LLMService llmService;

    @PostMapping(value = "/chat/completions", 
                produces = MediaType.APPLICATION_JSON_VALUE,
                consumes = MediaType.APPLICATION_JSON_VALUE) uest,
    public Mono<ResponseEntity<AIResponse>> chatCompletion(@RequestBody AIRequest request,                  @RequestHeader(value = "X-User-Language", defaultValue = "en") String userLanguage) {
                                                         @RequestHeader(value = "X-User-Language", defaultValue = "en") String userLanguage) {;
        log.info("Chat completion request received with language: {}", userLanguage);request.setRequestType("CHAT");
        request.setRequestType("CHAT");dil tercihini request'e ekle
        request.setLanguage(userLanguage); // Kullanıcı dil tercihini request'e ekle
        request)
        return llmService.processChatCompletion(request)pletion success"))
            .doOnSuccess(response -> log.debug("Chat completion success"))ponseEntity.ok()
            .map(response -> ResponseEntity.ok()aType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .body(response))
            .doOnError(error -> {r: {}", error.getMessage(), error);
                log.error("Chat completion error: {}", error.getMessage(), error);
                // Detaylı hata loguf (error.getCause() != null) {
                if (error.getCause() != null) {      log.error("Root cause: {}", error.getCause().getMessage());
                    log.error("Root cause: {}", error.getCause().getMessage());
                }
            })
            .onErrorResume(error -> {ssage();
                String errorMessage = error.getMessage();ssage.contains("API hatası:")) {
                if (errorMessage != null && errorMessage.contains("API hatası:")) {
                    // API hatasını doğrudan ilet
                    return Mono.just(ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .status(HttpStatus.BAD_GATEWAY).error(errorMessage)));
                        .body(AIResponse.error(errorMessage)));
                } else {
                    // Genel hata mesajı
                    return Mono.just(ResponseEntity       .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)         .body(AIResponse.error("LLM servisinde bir hata oluştu: " + errorMessage)));
                        .body(AIResponse.error("LLM servisinde bir hata oluştu: " + errorMessage)));           }
                }            });
            });
    }

    @PostMapping(value = "/chat/completions/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE) 
    public Flux<StreamResponse> streamChatCompletion(@RequestBody AIRequest request,est,
                                                  @RequestHeader(value = "X-User-Language", defaultValue = "en") String userLanguage) {-Language", defaultValue = "en") String userLanguage,
        log.info("Stream chat completion request received with language: {}", userLanguage);    @RequestHeader(value = "X-Request-ID", required = false) String requestId,
        request.setRequestType("CHAT");
        request.setLanguage(userLanguage);
        st received with language: {}, requestId: {}", 
        return llmService.streamChatCompletion(request)
            // Yanıtları hemen istemciye gönder - immediate scheduler ile
            .publishOn(Schedulers.immediate())
            // Backpressure stratejisi - DROP ile aşırı yük durumunda bazı yanıtları atlayabilirguage);
            // ama gecikme olmaz
            .onBackpressureDrop(item -> log.warn("Dropped stream item due to backpressure"))
            .doOnNext(response -> {Complete()
                if (log.isDebugEnabled()) {
                    log.debug("Sending chunk immediately: {}", ncelling stream for request: {}", requestId)));
                        response.getContent().length() > 20 
                            ? response.getContent().substring(0, 20) + "..."  llmService.streamChatCompletion(request)
                            : response.getContent());
                }
            })       // Akışı SSE formatına dönüştür
            .doOnError(error -> log.error("Stream error: {}", error.getMessage(), error))            .map(chunk -> ServerSentEvent.<StreamResponse>builder()
            .doOnComplete(() -> log.info("Stream completed for request"));equestId : UUID.randomUUID().toString())
    }

    @PostMapping(value = "/images/generate", 
                produces = MediaType.APPLICATION_JSON_VALUE,e akışı iptal et
                consumes = MediaType.APPLICATION_JSON_VALUE) 
    public Mono<ResponseEntity<AIResponse>> generateImage(@RequestBody AIRequest request) {e aşırı yük durumunda bazı yanıtları atlayabilir
        request.setRequestType("IMAGE");d stream item due to backpressure"))
        return llmService.processImageGeneration(request)> {
            .map(response -> ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response))               log.debug("Sending chunk: {}", 
            .doOnError(error -> log.error("Image generation error: ", error))                        content != null && content.length() > 20 
            .doOnSuccess(response -> log.debug("Image generation success: {}", response));ng(0, 20) + "..." 
    }

    @PostMapping(value = "/code/completion", 
                produces = MediaType.APPLICATION_JSON_VALUE,sajı için özel SSE yorumu ekle
                consumes = MediaType.APPLICATION_JSON_VALUE) uilder()
    public Mono<ResponseEntity<AIResponse>> codeCompletion(@RequestBody AIRequest request) {ING")
        request.setRequestType("CODE");
        return llmService.processCodeCompletion(request)log.error("Stream error: {}", error.getMessage(), error))
            .map(response -> ResponseEntity.ok() requestId))
                .contentType(MediaType.APPLICATION_JSON)
                .body(response))           log.error("Stream error occurred: {}", error.getMessage(), error);
                return Flux.just(ServerSentEvent.<StreamResponse>builder()            .doOnError(error -> log.error("Code completion error: ", error))
            .doOnSuccess(response -> log.debug("Code completion success: {}", response));
    }
Message())
    @PostMapping(value = "/enhance-prompt", 
                produces = MediaType.APPLICATION_JSON_VALUE,
                consumes = MediaType.APPLICATION_JSON_VALUE) 
    public Mono<ResponseEntity<AIResponse>> enhancePrompt(@RequestBody AIRequest request,
                                                        @RequestHeader(value = "X-User-Language", defaultValue = "en") String userLanguage) {
        log.info("Prompt enhancement request received with language: {}", userLanguage);
        request.setRequestType("ENHANCE");
        request.setLanguage(userLanguage);
        _JSON_VALUE) 
        return llmService.enhancePrompt(request)e(@RequestBody AIRequest request) {
            .doOnSuccess(response -> log.debug("Prompt enhancement success"))IMAGE");
            .map(response -> ResponseEntity.ok()ImageGeneration(request)
                .contentType(MediaType.APPLICATION_JSON)
                .body(response))ION_JSON)
            .doOnError(error -> {
                log.error("Prompt enhancement error: {}", error.getMessage(), error);Error(error -> log.error("Image generation error: ", error))
                if (error.getCause() != null) {oOnSuccess(response -> log.debug("Image generation success: {}", response));
                    log.error("Root cause: {}", error.getCause().getMessage());
                }
            })
            .onErrorResume(error -> {ON_VALUE,
                String errorMessage = error.getMessage();ALUE) 
                if (errorMessage != null && errorMessage.contains("API hatası:")) {
                    return Mono.just(ResponseEntitystType("CODE");
                        .status(HttpStatus.BAD_GATEWAY)uest)
                        .body(AIResponse.error("Prompt iyileştirilemedi: " + errorMessage)));
                } else {
                    return Mono.just(ResponseEntitybody(response))
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)OnError(error -> log.error("Code completion error: ", error))
                        .body(AIResponse.error("Prompt iyileştirilemedi: " + errorMessage)));       .doOnSuccess(response -> log.debug("Code completion success: {}", response));
                }   }



}    }            });































}    }            });                }                        .body(AIResponse.error("Prompt iyileştirilemedi: " + errorMessage)));                        .status(HttpStatus.INTERNAL_SERVER_ERROR)                    return Mono.just(ResponseEntity                } else {                        .body(AIResponse.error("Prompt iyileştirilemedi: " + errorMessage)));                        .status(HttpStatus.BAD_GATEWAY)                    return Mono.just(ResponseEntity                if (errorMessage != null && errorMessage.contains("API hatası:")) {                String errorMessage = error.getMessage();            .onErrorResume(error -> {            })                }                    log.error("Root cause: {}", error.getCause().getMessage());                if (error.getCause() != null) {                log.error("Prompt enhancement error: {}", error.getMessage(), error);            .doOnError(error -> {                .body(response))                .contentType(MediaType.APPLICATION_JSON)            .map(response -> ResponseEntity.ok()            .doOnSuccess(response -> log.debug("Prompt enhancement success"))        return llmService.enhancePrompt(request)                request.setLanguage(userLanguage);        request.setRequestType("ENHANCE");        log.info("Prompt enhancement request received with language: {}", userLanguage);                                                        @RequestHeader(value = "X-User-Language", defaultValue = "en") String userLanguage) {    public Mono<ResponseEntity<AIResponse>> enhancePrompt(@RequestBody AIRequest request,    @PostMapping(value = "/enhance-prompt", 
                produces = MediaType.APPLICATION_JSON_VALUE,
                consumes = MediaType.APPLICATION_JSON_VALUE) 