package com.craftpilot.imageservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageHistory {
    private String id;
    private String userId;
    private List<Image> images;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
} 