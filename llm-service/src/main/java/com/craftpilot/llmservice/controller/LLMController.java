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
            // İşlem takibi için içerik biriktirme buffer'ı
            final StringBuilder contentBuffer = new StringBuilder();
            // Tablo algılama için flag
            final boolean[] tableDetected = {false};
            final boolean[] tableIssueDetected = {false};
            
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
                        
                        // Tablo yapısını algıla
                        if (optimizedContent != null && optimizedContent.contains("|")) {
                            tableDetected[0] = true;
                            
                            // Sorunlu tablo algılama (çok uzun hücre içeren tablolar)
                            if (optimizedContent.contains("|") && optimizedContent.length() > 500 && 
                                optimizedContent.split("\\|").length > 2) {
                                int cellLength = optimizedContent.split("\\|")[1].trim().length();
                                if (cellLength > 300) {
                                    tableIssueDetected[0] = true;
                                    log.warn("Potentially broken table format detected: cell length = {}", cellLength);
                                }
                            }
                        }
                        
                        // İçeriği buffer'a ekle ve gönder
                        if (optimizedContent != null && !optimizedContent.isEmpty()) {
                            // Buffer'da içeriği biriktir
                            contentBuffer.append(optimizedContent);
                            
                            // İçeriği güncelle ve gönder
                            StreamResponse optimizedChunk = StreamResponse.builder()
                                .content(optimizedContent)
                                .done(chunk.isDone())
                                .error(chunk.isError())
                                .build();
                            
                            sink.next(ServerSentEvent.<StreamResponse>builder()
                                .id(trackingId)
                                .event(chunk.isError() ? "error" : "message")
                                .data(optimizedChunk)
                                .build());
                        }
                        
                        // Tamamlanma sinyali geldiğinde son bir kontrol
                        if (chunk.isDone()) {
                            // Eğer sorunlu tablo algılandıysa düzeltme için yeni istek gönder
                            if (tableDetected[0] && tableIssueDetected[0]) {
                                String originalContent = contentBuffer.toString();
                                
                                // Düzeltme için yeni istek oluştur
                                StreamResponse fixRequest = StreamResponse.builder()
                                    .content("\n\n_Not: Tablo formatı sorunlu görünüyor. Düzeltilmiş bir tablo formatı gönderiliyor..._")
                                    .done(false)
                                    .error(false)
                                    .build();
                                
                                sink.next(ServerSentEvent.<StreamResponse>builder()
                                    .id(trackingId)
                                    .event("message")
                                    .data(fixRequest)
                                    .build());
                                
                                // Yeni bir istek oluştur ve düzeltilmiş tablo için gönder
                                fixTableFormat(request, originalContent, trackingId)
                                    .doOnNext(fixedChunk -> {
                                        sink.next(ServerSentEvent.<StreamResponse>builder()
                                            .id(trackingId)
                                            .event("message")
                                            .data(fixedChunk)
                                            .build());
                                    })
                                    .doOnComplete(() -> {
                                        // Son olarak düzeltilmiş yanıtın tamamlandığını bildir
                                        sink.next(ServerSentEvent.<StreamResponse>builder()
                                            .id(trackingId)
                                            .event("message")
                                            .data(StreamResponse.builder()
                                                .content("")
                                                .done(true)
                                                .build())
                                            .build());
                                    })
                                    .subscribe();
                            } else {
                                // Son olarak tamamlanma sinyalini gönder
                                sink.next(ServerSentEvent.<StreamResponse>builder()
                                    .id(trackingId)
                                    .event(chunk.isError() ? "error" : "message")
                                    .data(chunk)
                                    .build());
                            }
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
    
    /**
     * Tablo veya büyük metin içeren içeriği algılayan ve optimize eden yardımcı metod
     * @param content Optimize edilecek içerik
     * @return Optimize edilmiş içerik
     */
    private String optimizeStreamContent(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        
        // Tablo satırı gibi çok fazla boşluk içeren satırları optimize et
        if (content.contains("|") && content.length() > 200) {
            // Çok fazla ardışık boşluk karakterini temizle (20'den fazla boşluğu 1 boşluğa indirgeme)
            return content.replaceAll(" {20,}", " ");
        }
        
        // Çok uzun boş satırları temizle (tamamen boşluk karakterlerinden oluşan satırlar)
        if (content.trim().isEmpty() && content.length() > 100) {
            return "";
        }
        
        return content;
    }
    
    /**
     * Stream yanıtlarını birleştiren yardımcı metod
     * @param streamBuffer Şu ana kadar toplanan içerik
     * @param newContent Yeni gelen içerik
     * @return Birleştirilmiş içerik
     */
    private String mergeStreamContents(String streamBuffer, String newContent) {
        if (streamBuffer == null) streamBuffer = "";
        if (newContent == null) newContent = "";
        
        return streamBuffer + newContent;
    }

    /**
     * Sorunlu tablo formatını düzeltmek için yeni bir istek oluşturur
     * @param originalRequest Orijinal istek
     * @param originalContent Orijinal yanıt içeriği
     * @param trackingId Talep izleme ID'si
     * @return Düzeltilmiş içerik stream'i
     */
    private Flux<StreamResponse> fixTableFormat(AIRequest originalRequest, String originalContent, String trackingId) {
        // Yeni bir istek için AIRequest oluştur
        AIRequest fixRequest = new AIRequest();
        fixRequest.setRequestId(trackingId + "-fix");
        fixRequest.setRequestType("CHAT");
        fixRequest.setLanguage(originalRequest.getLanguage());
        fixRequest.setModel("google/gemini-2.0-flash-001"); // Daha yeni model kullan
        
        // Tablo düzeltme prompt'u oluştur
        String fixPrompt = "Aşağıdaki içerikteki tabloyu markdown formatında, daha sade ve okunabilir bir şekilde yeniden düzenle. " +
            "Tüm içeriği aynen koruyarak sadece tablo formatını düzelt. Tablo hücrelerindeki içerikler çok uzun olmamalı ve düzgün görüntülenmelidir:\n\n" + 
            originalContent;
        
        fixRequest.setPrompt(fixPrompt);
        fixRequest.setTemperature(0.2f); // Daha düşük ısı değeri belirle - daha tutarlı çıktı için
        
        log.info("Sending table format fix request with ID: {}", fixRequest.getRequestId());
        
        // Yeni içerik almak için LLM'e istek gönder
        return llmService.streamChatCompletion(fixRequest)
            .filter(chunk -> !chunk.isPing() && chunk.getContent() != null && !chunk.getContent().isEmpty())
            .map(chunk -> {
                String content = chunk.getContent();
                
                // Eğer uzun hücreler hala varsa temizle
                if (content.contains("|")) {
                    content = optimizeTableContent(content);
                }
                
                return StreamResponse.builder()
                    .content(content)
                    .done(chunk.isDone())
                    .error(chunk.isError())
                    .build();
            });
    }

    /**
     * Tablo içeriğini optimize eder, çok uzun hücreleri kısaltır
     * @param tableContent Tablo içeriği
     * @return Optimize edilmiş tablo içeriği
     */
    private String optimizeTableContent(String tableContent) {
        // Çok uzun satırları kısalt
        String[] lines = tableContent.split("\n");
        StringBuilder optimized = new StringBuilder();
        
        for (String line : lines) {
            if (line.contains("|")) {
                String[] cells = line.split("\\|");
                StringBuilder newLine = new StringBuilder();
                
                for (int i = 0; i < cells.length; i++) {
                    String cell = cells[i];
                    // Hücre içeriğini makul bir uzunluğa kısalt (100 karakter)
                    if (cell.trim().length() > 100) {
                        cell = cell.trim().substring(0, 97) + "...";
                    }
                    newLine.append("|").append(cell);
                }
                
                // Son hücre için | ekle
                if (!line.endsWith("|")) {
                    newLine.append("|");
                }
                
                optimized.append(newLine).append("\n");
            } else {
                optimized.append(line).append("\n");
            }
        }
        
        return optimized.toString();
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
}
```