package com.craftpilot.imageservice.service;

import com.craftpilot.imageservice.model.Image;
import com.craftpilot.imageservice.repository.ImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ImageService {
    private final ImageRepository imageRepository;
    private final OpenAIService openAIService;
    private final KafkaTemplate<String, ImageEvent> kafkaTemplate;

    @Value("${kafka.topics.image-events}")
    private String imageEventsTopic;

    public Mono<Image> generateImage(Image image) {
        return openAIService.generateImage(image.getPrompt())
                .map(imageUrl -> {
                    image.setImageUrl(imageUrl);
                    return image;
                })
                .flatMap(imageRepository::save)
                .doOnSuccess(savedImage -> {
                    ImageEvent event = ImageEvent.fromImage(savedImage, "IMAGE_GENERATED");
                    kafkaTemplate.send(imageEventsTopic, savedImage.getId(), event)
                            .addCallback(
                                result -> log.info("Image generation event sent successfully for image: {}", savedImage.getId()),
                                ex -> log.error("Failed to send image generation event", ex)
                            );
                });
    }

    public Mono<Image> getImage(String id) {
        return imageRepository.findById(id);
    }

    public Flux<Image> getUserImages(String userId) {
        return imageRepository.findByUserId(userId);
    }

    public Mono<Void> deleteImage(String id) {
        return imageRepository.findById(id)
                .flatMap(image -> {
                    ImageEvent event = ImageEvent.fromImage(image, "IMAGE_DELETED");
                    return imageRepository.deleteById(id)
                            .then(Mono.fromRunnable(() -> 
                                kafkaTemplate.send(imageEventsTopic, id, event)
                                    .addCallback(
                                        result -> log.info("Image deletion event sent successfully for image: {}", id),
                                        ex -> log.error("Failed to send image deletion event", ex)
                                    )
                            ));
                });
    }
}