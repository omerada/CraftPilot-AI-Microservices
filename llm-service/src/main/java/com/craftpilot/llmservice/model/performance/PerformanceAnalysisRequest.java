package com.craftpilot.llmservice.model.performance;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceAnalysisRequest {
    @NotBlank(message = "URL cannot be empty")
    private String url;
    
    @Builder.Default
    private String analysisType = "basic"; // Varsayılan olarak "basic", diğer değer "detailed"
    
    @Builder.Default
    private String deviceType = "desktop"; // Cihaz tipi alanı ekle
}
