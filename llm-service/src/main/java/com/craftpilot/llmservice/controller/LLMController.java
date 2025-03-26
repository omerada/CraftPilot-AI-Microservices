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
                        
                        // Tablolarla ilgili yanıtlarda buffer'a ekle ve gönder
                        if (optimizedContent != null && !optimizedContent.isEmpty()) {
                            // Buffer'da içeriği biriktir ve birleştir
                            String mergedContent = mergeStreamContents(contentBuffer.toString(), optimizedContent);
                            contentBuffer.setLength(0);
                            contentBuffer.append(mergedContent);
                            
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
                            // Eğer içerik tablo yapısı içeriyor ve yarım kalmış gibi görünüyorsa 
                            // ek bilgi gönderelim
                            if (contentBuffer.toString().contains("|") && 
                                isTableIncomplete(contentBuffer.toString())) {
                                log.info("Tablo yapısı tamamlanmadan yanıt sonlandı");
                                
                                // Tablo için bir kapanış notu gönder
                                StreamResponse finalNote = StreamResponse.builder()
                                    .content("\n\n_Not: Tablo yapısı tamamlanamadı, yanıt erken sonlandı._")
                                    .done(false)
                                    .error(false)
                                    .build();
                                
                                sink.next(ServerSentEvent.<StreamResponse>builder()
                                    .id(trackingId)
                                    .event("message")
                                    .data(finalNote)
                                    .build());
                            }
                            
                            // Son olarak tamamlanma sinyalini gönder
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
     * Stream yanıtlarını birleştirerek tam tablo yapılarını tespit etmeye çalışan yardımcı metod
     * @param streamBuffer Şu ana kadar toplanan içerik
     * @param newContent Yeni gelen içerik
     * @return Birleştirilmiş ve gerekirse düzeltilmiş içerik
     */
    private String mergeStreamContents(String streamBuffer, String newContent) {
        if (streamBuffer == null) streamBuffer = "";
        if (newContent == null) newContent = "";
        
        // Tablo parçaları geliyorsa ve henüz tamamlanmamışsa işlemi geliştir
        String merged = streamBuffer + newContent;
        
        // Tablo yapısı algılandı ancak sonlanma belirtileri mevcut değilse
        // ve model yanıtı erkenden kesti ise, eksik tabloya bir sonlanma ekle
        if (merged.contains("|") && merged.contains("\n") && merged.endsWith("\n")) {
            // Son satır kontrolü
            String[] lines = merged.split("\n");
            String lastLine = lines.length > 0 ? lines[lines.length - 1] : "";
            
            // Son satır yarım kalmış bir tablo satırı ise düzelt
            if (lastLine.contains("|") && lastLine.trim().endsWith("|")) {
                log.info("Tablo yapısı tespit edildi ve son satır düzenleniyor: {}", lastLine);
                return merged;
            }
        }
        
        return merged;
    }

    /**
     * Bir tablonun tamamlanıp tamamlanmadığını kontrol eder
     * @param content Kontrol edilecek içerik
     * @return Tablo tamamlanmamışsa true, tamamlanmışsa false
     */
    private boolean isTableIncomplete(String content) {
        // İçerik boşsa veya tablo değilse tamamlanmamış kabul etme
        if (content == null || content.isEmpty() || !content.contains("|")) {
            return false;
        }
        
        // Satırlara ayır
        String[] lines = content.split("\n");
        if (lines.length < 3) {
            // En az başlık satırı, ayırıcı satır ve bir veri satırı olmalı
            return true;
        }
        
        // İlk satırdaki sütun sayısını referans al
        int firstRowColumns = countColumns(lines[0]);
        
        // Tablo satırlarını kontrol et
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            
            // Boş satırları atla
            if (line.isEmpty()) continue;
            
            // Tablo satırı ise kontrol et
            if (line.contains("|")) {
                // Satırın sütun sayısı ilk satırdan farklıysa veya satır yarım kalmışsa
                int columns = countColumns(line);
                if (columns != firstRowColumns || line.endsWith("|")) {
                    // Son satır "|" ile bitiyorsa ve öncekinden farklı sayıda sütun varsa
                    // bu muhtemelen yarım kalmış bir satırdır
                    if (i == lines.length - 1 && columns < firstRowColumns) {
                        return true;
                    }
                }
            }
        }
        
        // Son satır düzgün bir tablo satırı değilse
        String lastLine = lines[lines.length - 1].trim();
        if (lastLine.isEmpty() || !lastLine.startsWith("|") || lastLine.endsWith("|")) {
            return false; // Tablo muhtemelen tamamlanmış, son satır farklı içerik olabilir
        }
        
        // Tüm kontroller geçildiyse tablo tamamlanmıştır
        return false;
    }

    /**
     * Bir tablo satırındaki sütun sayısını hesaplar
     * @param line Tablo satırı
     * @return Sütun sayısı
     */
    private int countColumns(String line) {
        if (line == null || !line.contains("|")) {
            return 0;
        }
        // "|" karakterlerini sayarak sütun sayısını hesapla (ilk ve son | karakterleri hariç)
        int count = 0;
        for (char c : line.toCharArray()) {
            if (c == '|') count++;
        }
        return Math.max(0, count - 1); // İlk ve son | için düzeltme
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