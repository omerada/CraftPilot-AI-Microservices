package com.craftpilot.imageservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "image_histories")
public class ImageHistory {
    @Id
    private String id;
    private String userId;
    private String imageId;
    private String action;
    private String prompt;
    private String details;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}