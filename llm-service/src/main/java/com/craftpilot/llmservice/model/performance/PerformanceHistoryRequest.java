package com.craftpilot.llmservice.model.performance;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PerformanceHistoryRequest {
    @NotBlank(message = "URL boş olamaz")
    private String url;
}
