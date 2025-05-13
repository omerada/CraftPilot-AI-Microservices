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
@Document(collection = "images")
public class Image {
    @Id
    private String id;
    private String userId;
    private String prompt;
    private String imageUrl;
    private String model;
    private String size;
    private String quality;
    private String style;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}