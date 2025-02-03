package com.craftpilot.codeservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Code {
    private String id;
    private String userId;
    private String prompt;
    private String generatedCode;
    private String language;
    private String framework;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;
    private String model;
    private Double temperature;
    private Integer maxTokens;
} 