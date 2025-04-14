package com.craftpilot.llmservice.model.performance;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PerformanceAnalysisRequest {
    @NotBlank(message = "URL boş olamaz")
    private String url;
}
