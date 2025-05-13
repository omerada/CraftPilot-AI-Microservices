package com.craftpilot.imageservice.service;

import com.craftpilot.imageservice.model.Image;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Fallback mekanizmaları içeren ImageService özelliklerini sağlar.
 * Bağlantı sorunları yaşandığında servisin çalışmaya devam etmesini sağlar.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageServiceFallback {
    
    private final ImageService imageService;
    
    @CircuitBreaker(name = "mongoService", fallbackMethod = "getImageFallback")
    public Mono<Image> getImage(String id) {
        return imageService.getImage(id);
    }
    
    private Mono<Image> getImageFallback(String id, Exception ex) {
        log.error("Error retrieving image {}, using fallback. Error: {}", id, ex.getMessage());
        return Mono.empty();
    }
    
    @CircuitBreaker(name = "mongoService", fallbackMethod = "getUserImagesFallback")
    public Flux<Image> getUserImages(String userId) {
        return imageService.getUserImages(userId);
    }
    
    private Flux<Image> getUserImagesFallback(String userId, Exception ex) {
        log.error("Error retrieving images for user {}, using fallback. Error: {}", userId, ex.getMessage());
        return Flux.empty();
    }
    
    @CircuitBreaker(name = "kafkaService", fallbackMethod = "generateImageFallback")
    public Mono<Image> generateImage(Image image) {
        return imageService.generateImage(image)
                .onErrorResume(e -> {
                    log.error("Error during image generation, attempting to recover: {}", e.getMessage());
                    return saveImageWithoutKafka(image);
                });
    }
    
    private Mono<Image> generateImageFallback(Image image, Exception ex) {
        log.error("Circuit breaker triggered for image generation, using fallback. Error: {}", ex.getMessage());
        return saveImageWithoutKafka(image);
    }
    
    private Mono<Image> saveImageWithoutKafka(Image image) {
        log.info("Saving image without Kafka event publication");
        // Implement a direct save without Kafka event publication
        return Mono.just(image)
                .flatMap(img -> Mono.defer(() -> {
                    // Add image URL from fallback or placeholder if OpenAI integration failed
                    if (img.getImageUrl() == null || img.getImageUrl().isEmpty()) {
                        img.setImageUrl("https://placeholder.com/placeholder.jpg");
                    }
                    return imageService.getImage(img.getId()).defaultIfEmpty(img);
                }));
    }
}
