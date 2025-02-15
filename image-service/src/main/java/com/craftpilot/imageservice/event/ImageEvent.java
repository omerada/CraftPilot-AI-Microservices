package com.craftpilot.imageservice.event;

import com.craftpilot.imageservice.model.Image;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageEvent {
    private String eventId;
    private String userId;
    private String eventType;
    private String imageId;
    private String imageUrl;
    private String prompt;
    private LocalDateTime timestamp;
    private Image image;

    public static ImageEvent fromImage(Image image, String eventType) {
        return ImageEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .userId(image.getUserId())
                .eventType(eventType)
                .imageId(image.getId())
                .imageUrl(image.getImageUrl())
                .prompt(image.getPrompt())
                .timestamp(LocalDateTime.now())
                .image(image)
                .build();
    }
}
