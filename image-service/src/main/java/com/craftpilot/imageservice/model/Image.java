package com.craftpilot.imageservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Image {
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