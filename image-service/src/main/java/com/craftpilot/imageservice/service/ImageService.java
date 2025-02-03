package com.craftpilot.imageservice.service;

import com.craftpilot.imageservice.model.Image;
import com.craftpilot.imageservice.repository.ImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ImageService {
    private final ImageRepository imageRepository;
    private final OpenAIService openAIService;

    public Mono<Image> generateImage(Image image) {
        return openAIService.generateImage(image.getPrompt())
                .map(imageUrl -> {
                    image.setImageUrl(imageUrl);
                    return image;
                })
                .flatMap(imageRepository::save);
    }

    public Mono<Image> getImage(String id) {
        return imageRepository.findById(id);
    }

    public Flux<Image> getUserImages(String userId) {
        return imageRepository.findByUserId(userId);
    }

    public Mono<Void> deleteImage(String id) {
        return imageRepository.deleteById(id);
    }
} 