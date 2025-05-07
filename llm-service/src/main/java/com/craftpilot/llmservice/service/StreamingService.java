package com.craftpilot.llmservice.service;

import com.craftpilot.llmservice.config.OpenRouterProperties;
import com.craftpilot.llmservice.model.AIRequest;
import com.craftpilot.llmservice.model.StreamResponse;
import com.craftpilot.llmservice.service.client.OpenRouterClient;
import com.craftpilot.llmservice.util.ResponseExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Stream tabanlı AI yanıtlarını yönetir
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StreamingService {
    private final OpenRouterClient openRouterClient;
    private final ResponseExtractor responseExtractor;
    private final OpenRouterProperties properties;

    /**
     * AI isteğini stream olarak işler
     */
    public Flux<StreamResponse> streamChatCompletion(AIRequest request) {
        // Check if we need to add the system prompt
        if (request.getSystemPrompt() == null || request.getSystemPrompt().trim().isEmpty()) {
            // Add the system prompt to the request
            request.setSystemPrompt(properties.getDefaultSystemPrompt()); 
        }
         
        // Timeout yanıtı
        StreamResponse timeoutResponse = StreamResponse.builder()
            .content("Stream timeout occurred after " + properties.getStreamTimeoutSeconds() + " seconds")
            .done(true)
            .error(true)
            .build();
        
        // Timeout Flux
        List<StreamResponse> timeoutList = Collections.singletonList(timeoutResponse);
        Flux<StreamResponse> timeoutFlux = Flux.fromIterable(timeoutList);
        
        // JSON parçalarını birleştirmek için
        AtomicReference<StringBuilder> jsonBuffer = new AtomicReference<>(new StringBuilder());
        
        return Flux.<StreamResponse>create(sink -> {
             
            // İstemciye periyodik ping göndermek için timer başlat
            Disposable keepAliveTicker = Flux.interval(Duration.ofSeconds(properties.getKeepAliveIntervalSeconds()))
                .doOnNext(tick -> {
                     sink.next(StreamResponse.builder()
                        .content("")
                        .done(false)
                        .ping(true)   
                        .build());
                })
                .subscribe();
            
            // OpenRouter'dan stream al
            openRouterClient.streamFromOpenRouter(request)
                .doOnComplete(() -> {
                      keepAliveTicker.dispose();
                    
                    // Eğer biriken JSON varsa, son bir işleme deneyin
                    if (jsonBuffer.get().length() > 0) {
                        try {
                            String finalJson = jsonBuffer.get().toString();
                            responseExtractor.extractAndSendContent(finalJson, sink);
                            jsonBuffer.set(new StringBuilder());
                        } catch (Exception e) {
                            log.warn("Could not process final buffer: {}", e.getMessage());
                        }
                    }
                    
                    sink.next(StreamResponse.builder()
                        .content("")
                        .done(true)
                        .build());
                    sink.complete();
                })
                .doOnCancel(() -> {
                     keepAliveTicker.dispose();
                    sink.complete();
                })
                .doOnError(e -> {
                    log.error("OpenRouter stream error: {}", e.getMessage(), e);
                    keepAliveTicker.dispose();
                    
                    sink.next(StreamResponse.builder()
                        .content("Error: " + e.getMessage())
                        .done(true)
                        .error(true)
                        .build());
                    sink.complete();
                })
                .subscribe(chunk -> {
                     
                    if (chunk == null || chunk.isEmpty()) {
                         return;
                    }
                    
                    // SSE veri formatını işle
                    if (chunk.startsWith("data:")) {
                        String data = chunk.substring(5).trim();
                        
                        if ("[DONE]".equals(data)) {
                            sink.next(StreamResponse.builder()
                                .content("")
                                .done(true)
                                .build());
                            return;
                        }
                        
                        // Chunk'ın içeriğini JSON buffer'a ekle
                        try {
                            // Contentte JSON olup olmadığını kontrol et
                            responseExtractor.extractAndSendContent(data, sink);
                        } catch (Exception e) {
                            // Parse hatası, buffer'a ekle
                            StringBuilder buffer = jsonBuffer.get();
                            buffer.append(data);
                            jsonBuffer.set(buffer);
                            
                            try {
                                // Tamam mı kontrol et
                                responseExtractor.extractAndSendContent(buffer.toString(), sink);
                                // Başarılı ise buffer'ı temizle
                                jsonBuffer.set(new StringBuilder());
                            } catch (Exception e2) {
                                // Hala eksik 
                            }
                        }
                    } else if (chunk.startsWith(":")) {
                        // Yorum veya keep-alive, görmezden gel 
                    } else {
                        // Non-SSE format, may be direct content
                        StringBuilder buffer = jsonBuffer.get();
                        buffer.append(chunk);
                        jsonBuffer.set(buffer);
                        
                        try {
                            // Topladığımız verileri işlemeyi dene
                            String bufferContent = buffer.toString();
                            responseExtractor.extractAndSendContent(bufferContent, sink);
                            jsonBuffer.set(new StringBuilder());
                        } catch (Exception e) {
                            log.debug("Buffer not yet complete: {}", e.getMessage());
                        }
                    }
                });
        }, FluxSink.OverflowStrategy.BUFFER) 
        .onBackpressureBuffer(256)
        .timeout(Duration.ofSeconds(properties.getStreamTimeoutSeconds()), timeoutFlux); 
    }
}
