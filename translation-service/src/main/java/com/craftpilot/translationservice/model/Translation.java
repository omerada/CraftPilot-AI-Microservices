package com.craftpilot.translationservice.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Translation {
    private String id;
    private String userId;
    private String sourceText;
    private String translatedText;
    private String sourceLanguage;
    private String targetLanguage;
    private String provider;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
} 