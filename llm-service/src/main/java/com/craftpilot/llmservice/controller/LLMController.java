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
import java.util.regex.Pattern;

import com.craftpilot.llmservice.service.PromptService;
import com.craftpilot.llmservice.service.ChatService;
import com.craftpilot.llmservice.service.ChatEnhancementService;

@Slf4j
@RestController
@RequestMapping("/")  
@RequiredArgsConstructor 
public class LLMController {
    private final LLMService llmService;
    private final PromptService promptService;
    private final ChatService chatService;
    // ChatEnhancementService'i ekleyin
    private final ChatEnhancementService chatEnhancementService;
    // Aşırı uzun boşluk dizilerini tespit etmek için pattern
    private static final Pattern EXCESSIVE_WHITESPACE = Pattern.compile("\\s{100,}");
    // Maksimum izin verilen boşluk sayısı
    private static final int MAX_WHITESPACE = 1;

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
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            ServerWebExchange exchange) {
        
        // RequestID yoksa bir tane oluştur (loglamada null görmemek için)
        final String trackingId = requestId != null ? requestId : UUID.randomUUID().toString();
        
        log.info("Stream chat completion request received with language: {}, requestId: {}, model: {}, userId: {}", 
                userLanguage, trackingId, request.getModel(), userId);
        
        // Kullanıcı ID'sini AI isteğine ekle
        request.setUserId(userId);
        
        // Tablo yanıtı olabilecek özel anahtar kelimeleri tespit et
        boolean potentialTableResponse = request.getPrompt() != null && 
            (request.getPrompt().contains("tablo") || 
             request.getPrompt().contains("table") || 
             request.getPrompt().contains("karşılaştır") ||
             request.getPrompt().contains("compare"));
        
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
                        // İçeriği temizle - Özellikle tablo yanıtı olabilecek durumlarda
                        if (chunk.getContent() != null && !chunk.isDone()) {
                            String cleanedContent = chunk.getContent();
                            
                            // Aşırı uzun boşluk dizilerini temizle
                            if (cleanedContent.contains("    ") || cleanedContent.contains("\n\n\n")) {
                                cleanedContent = EXCESSIVE_WHITESPACE.matcher(cleanedContent)
                                    .replaceAll(" ".repeat(MAX_WHITESPACE));
                                
                                // Tablo yapısını kontrol et ve düzelt
                                if (potentialTableResponse && cleanedContent.contains("|")) {
                                    // Tablo formatını düzeltmeye çalış
                                    cleanedContent = cleanTableFormat(cleanedContent);
                                }
                                
                                chunk = StreamResponse.builder()
                                    .content(cleanedContent)
                                    .done(chunk.isDone())
                                    .error(chunk.isError())
                                    .build();
                            }
                        }
                        
                        // İçerikte JSON veriyorsa güzelce formatla
                        sink.next(ServerSentEvent.<StreamResponse>builder()
                            .id(trackingId)
                            .event(chunk.isError() ? "error" : "message")
                            .data(chunk)
                            .build());
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
    
    /**
     * Tablo formatını düzeltmeye yardımcı olacak yardımcı metod
     * @param content Düzeltilecek içerik
     * @return Düzeltilmiş tablo içeriği
     */
    private String cleanTableFormat(String content) {
        // Birden fazla satır varsa ve tablo formatı olduğunu düşünüyorsak
        if (content.contains("|")) {
            // Birden fazla boş satırı kaldır
            content = content.replaceAll("\\n{3,}", "\n\n");
            
            // Markdown tablo yapısını düzenle
            if (content.contains("| -") || content.contains("|-")) {
                // Tablo başlık satırlarını koruyalım, diğer içeriği düzenleyelim
                return content;
            } else if (content.contains("|")) {
                // Eğer çok uzun boşluklar içeren bir tablo satırı varsa, bunları temizleyelim
                return content.replaceAll("\\|\\s{10,}", "| ");
            }
        }
        return content;
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
        
        // Prompt kontrolü
        if (request.getPrompt() == null || request.getPrompt().trim().isEmpty()) {
            AIResponse errorResponse = AIResponse.builder()
                .response("İyileştirilecek bir prompt göndermelisiniz.")
                .success(false)
                .build();
            
            return Mono.just(ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse));
        }
        
        return llmService.enhancePrompt(request)
            .doOnSuccess(response -> {
                if (response == null) {
                    log.warn("Prompt enhancement returned null response");
                } else {
                    log.debug("Prompt enhancement success with response length: {}", 
                        response.getResponse() != null ? response.getResponse().length() : 0);
                }
            })
            .map(response -> {
                // Null kontrolü ve varsayılan değer atama
                if (response == null) {
                    response = AIResponse.builder()
                        .response("Prompt iyileştirme yanıtı alınamadı. Lütfen daha sonra tekrar deneyin.")
                        .requestId(request.getRequestId())
                        .success(false)
                        .build();
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

    @PostMapping("/chat")
    public Mono<ResponseEntity<AIResponse>> chat(@RequestBody AIRequest request,
                                             @RequestHeader(value = "X-User-Id", required = false) String userId,
                                             @RequestHeader(value = "X-User-Language", defaultValue = "en") String userLanguage) {
        log.info("Chat request received with language: {}", userLanguage);
        request.setRequestType("CHAT");
        request.setLanguage(userLanguage);

        // Model belirtilmemişse varsayılan bir model ata
        if (request.getModel() == null || request.getModel().isEmpty()) {
            request.setModel("google/gemini-pro");
        }

        // Yanıt üretildikten sonra kullanıcı bilgilerini işle
        return llmService.processChatCompletion(request)
                .doOnSuccess(response -> {
                    if (userId != null && request.getPrompt() != null) {
                        // Kullanıcı mesajından bilgileri çıkar ve kaydet
                        // Bu işlemi burada subscribe ederek garanti altına alıyoruz
                        chatEnhancementService.processUserMessage(userId, request.getPrompt(), "chat-request")
                                .subscribe(
                                        null,
                                        error -> log.error("Error processing user message: {}", error.getMessage())
                                );
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
}