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
        request.setLanguage(userLanguage);
        
        // Model belirtilmemişse varsayılan bir model ata
        if (request.getModel() == null || request.getModel().isEmpty()) {
            request.setModel("google/gemini-pro");
        }
        
        return llmService.processChatCompletion(request)
            .doOnSuccess(response -> {
                if (response == null) {
                    log.warn("Chat completion returned null response");
                } else {
                    log.debug("Chat completion success with response length: {}", 
                        response.getResponse() != null ? response.getResponse().length() : 0);
                }
            })
            .map(response -> {
                // Null kontrolü ve varsayılan değer atama
                if (response == null) {
                    response = AIResponse.builder()
                        .response("Servis yanıtı alınamadı. Lütfen daha sonra tekrar deneyin.")
                        .requestId(request.getRequestId())
                        .success(false)
                        .build();
                }
                
                return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response);
            })
            .doOnError(error -> log.error("Chat completion error: {}", error.getMessage(), error))
            .onErrorResume(error -> {
                AIResponse errorResponse = AIResponse.builder()
                    .response("İşlem sırasında bir hata oluştu: " + error.getMessage())
                    .requestId(request.getRequestId())
                    .success(false)
                    .build();
                
                HttpStatus status = (error instanceof APIException) ? 
                    HttpStatus.BAD_GATEWAY : HttpStatus.INTERNAL_SERVER_ERROR;
                
                return Mono.just(ResponseEntity
                    .status(status)
                    .body(errorResponse));
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
        
        log.info("Stream chat completion request received with language: {}, requestId: {}, model: {}", 
                userLanguage, trackingId, request.getModel());
        
        request.setRequestType("CHAT");
        request.setLanguage(userLanguage);
        
        // Ensure requestId is set for tracking
        if (request.getRequestId() == null) {
            request.setRequestId(trackingId);
        }
        
        // Response header'larını ayarla - bu header'lar bağlantının kesilmemesi için kritik
        exchange.getResponse().getHeaders().setContentType(MediaType.TEXT_EVENT_STREAM);
        exchange.getResponse().getHeaders().setCacheControl("no-cache, no-transform");
        exchange.getResponse().getHeaders().setConnection("keep-alive");
        exchange.getResponse().getHeaders().add("X-Accel-Buffering", "no"); // Nginx proxy için buffering kapatma
        
        // İstemci tarafı bağlantı kopması için daha güvenilir tespit
        exchange.getResponse().beforeCommit(() -> {
            log.debug("Response commit starting for request: {}", trackingId);
            return Mono.empty();
        });
        
        // Client'a yanıt göndermeye başlayalım
        return Flux.<ServerSentEvent<StreamResponse>>create(sink -> {
            // İlk olarak boş bir yorum gönder - bağlantıyı başlatmak için
            sink.next(ServerSentEvent.<StreamResponse>builder()
                .comment("OPENROUTER PROCESSING")
                .id(trackingId)
                .build());
            
            // Hemen ardından bir ping mesajı gönder
            sink.next(ServerSentEvent.<StreamResponse>builder()
                .id(trackingId)
                .event("ping")
                .data(StreamResponse.builder().content("").done(false).build())
                .build());
            
            // LLM servisi ile gerçek akışı başlat
            llmService.streamChatCompletion(request)
                .doOnNext(chunk -> {
                    // Log every chunk to track what we're getting from the service
                    log.debug("Stream chunk received from service: type={}, done={}, content={}",
                        chunk.isPing() ? "ping" : (chunk.isError() ? "error" : "content"),
                        chunk.isDone(),
                        chunk.getContent() != null ? 
                            (chunk.getContent().length() > 100 ? chunk.getContent().substring(0, 100) + "..." : chunk.getContent())
                            : "<null>");
                    
                    // Only forward non-ping chunks to the client (pings are for internal connection health)
                    if (!chunk.isPing()) {
                        // İçeriği optimize et
                        String optimizedContent = optimizeStreamContent(chunk.getContent());
                        
                        // Boş içeriği göndermekten kaçın
                        if (optimizedContent != null && !optimizedContent.isEmpty()) {
                            // İçeriği güncelle
                            StreamResponse optimizedChunk = StreamResponse.builder()
                                .content(optimizedContent)
                                .done(chunk.isDone())
                                .error(chunk.isError())
                                .build();
                            
                            // İçerikte JSON veriyorsa güzelce formatla
                            sink.next(ServerSentEvent.<StreamResponse>builder()
                                .id(trackingId)
                                .event(chunk.isError() ? "error" : "message")
                                .data(optimizedChunk)
                                .build());
                        } else if (chunk.isDone()) {
                            // Sadece done=true ise boş içerik gönder
                            sink.next(ServerSentEvent.<StreamResponse>builder()
                                .id(trackingId)
                                .event(chunk.isError() ? "error" : "message")
                                .data(chunk)
                                .build());
                        }
                    }
                })
                .doOnComplete(() -> {
                    log.info("LLM stream completed for request: {}", trackingId);
                    sink.complete();
                })
                .doOnError(error -> {
                    log.error("LLM stream error: {}", error.getMessage(), error);
                    sink.next(ServerSentEvent.<StreamResponse>builder()
                        .id(trackingId)
                        .event("error")
                        .data(StreamResponse.builder()
                            .content("Hata: " + error.getMessage())
                            .done(true)
                            .error(true)
                            .build())
                        .build());
                    sink.complete();
                })
                .subscribe();
        })
        .onBackpressureBuffer(256)
        .doOnCancel(() -> log.warn("Stream response was cancelled for request: {}", trackingId))
        .doOnComplete(() -> log.info("Stream response completed for request: {}", trackingId))
        .doOnError(error -> log.error("Stream response error: {}", error.getMessage(), error));
    }

    @PostMapping(value = "/images/generate", 
                produces = MediaType.APPLICATION_JSON_VALUE,
                consumes = MediaType.APPLICATION_JSON_VALUE) 
    public Mono<ResponseEntity<AIResponse>> generateImage(@RequestBody AIRequest request) {
        request.setRequestType("IMAGE");
        return llmService.processImageGeneration(request)
            .map(response -> ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)timize eden yardımcı metod
                .body(response))
            .doOnError(error -> log.error("Image generation error: ", error))
            .doOnSuccess(response -> log.debug("Image generation success: {}", response));
    }
mpty()) {
    @PostMapping(value = "/code/completion", 
                produces = MediaType.APPLICATION_JSON_VALUE,}
                consumes = MediaType.APPLICATION_JSON_VALUE) 
    public Mono<ResponseEntity<AIResponse>> codeCompletion(@RequestBody AIRequest request) {
        request.setRequestType("CODE");) {
        return llmService.processCodeCompletion(request)a boşluğu 1 boşluğa indirgeme)
            .map(response -> ResponseEntity.ok()aceAll(" {20,}", " ");
                .contentType(MediaType.APPLICATION_JSON)
                .body(response))
            .doOnError(error -> log.error("Code completion error: ", error))tamamen boşluk karakterlerinden oluşan satırlar)
            .doOnSuccess(response -> log.debug("Code completion success: {}", response));.length() > 100) {
    }

    @PostMapping(value = "/enhance-prompt", 
                produces = MediaType.APPLICATION_JSON_VALUE,
                consumes = MediaType.APPLICATION_JSON_VALUE) 
    public Mono<ResponseEntity<AIResponse>> enhancePrompt(@RequestBody AIRequest request,
                                                        @RequestHeader(value = "X-User-Language", defaultValue = "en") String userLanguage) {
        log.info("Prompt enhancement request received with language: {}", userLanguage); = MediaType.APPLICATION_JSON_VALUE,
        request.setRequestType("ENHANCE");
        request.setLanguage(userLanguage);
                                               @RequestHeader(value = "X-User-Language", defaultValue = "en") String userLanguage) {
        // Prompt kontrolüfo("Prompt enhancement request received with language: {}", userLanguage);
        if (request.getPrompt() == null || request.getPrompt().trim().isEmpty()) {("ENHANCE");
            AIResponse errorResponse = AIResponse.builder()
                .response("İyileştirilecek bir prompt göndermelisiniz.")
                .success(false)
                .build();
            )
            return Mono.just(ResponseEntitycek bir prompt göndermelisiniz.")
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse));build();
        }
        y
        return llmService.enhancePrompt(request)
            .doOnSuccess(response -> {);
                if (response == null) {
                    log.warn("Prompt enhancement returned null response");
                } else {pt(request)
                    log.debug("Prompt enhancement success with response length: {}", 
                        response.getResponse() != null ? response.getResponse().length() : 0);
                }ned null response");
            })
            .map(response -> {("Prompt enhancement success with response length: {}", 
                // Null kontrolü ve varsayılan değer atama        response.getResponse() != null ? response.getResponse().length() : 0);
                if (response == null) {
                    response = AIResponse.builder()
                        .response("Prompt iyileştirme yanıtı alınamadı. Lütfen daha sonra tekrar deneyin.")(response -> {
                        .requestId(request.getRequestId())değer atama
                        .success(false)l) {
                        .build();builder()
                }         .response("Prompt iyileştirme yanıtı alınamadı. Lütfen daha sonra tekrar deneyin.")
                                   .requestId(request.getRequestId())
                return ResponseEntity.ok()                       .success(false)




















}    }            });                    .body(errorResponse));                    .status(status)                return Mono.just(ResponseEntity                                    HttpStatus.BAD_GATEWAY : HttpStatus.INTERNAL_SERVER_ERROR;                HttpStatus status = (error instanceof APIException) ?                                     .build();                    .success(false)                    .requestId(request.getRequestId())                    .response("Prompt iyileştirme sırasında bir hata oluştu: " + error.getMessage())                AIResponse errorResponse = AIResponse.builder()            .onErrorResume(error -> {            .doOnError(error -> log.error("Prompt enhancement error: {}", error.getMessage(), error))            })                    .body(response);                    .contentType(MediaType.APPLICATION_JSON)                        .build();
                }
                
                return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response);
            })
            .doOnError(error -> log.error("Prompt enhancement error: {}", error.getMessage(), error))
            .onErrorResume(error -> {
                AIResponse errorResponse = AIResponse.builder()
                    .response("Prompt iyileştirme sırasında bir hata oluştu: " + error.getMessage())
                    .requestId(request.getRequestId())
                    .success(false)
                    .build();
                
                HttpStatus status = (error instanceof APIException) ? 
                    HttpStatus.BAD_GATEWAY : HttpStatus.INTERNAL_SERVER_ERROR;
                
                return Mono.just(ResponseEntity
                    .status(status)
                    .body(errorResponse));
            });
    }
}