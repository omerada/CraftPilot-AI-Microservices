package com.craftpilot.lighthouseservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisRequest {
    @NotBlank(message = "URL cannot be empty")
    @Pattern(regexp = "^(https?://)[a-zA-Z0-9][a-zA-Z0-9-]{0,61}[a-zA-Z0-9](?:\\.[a-zA-Z]{2,})+.*$", 
             message = "Invalid URL format. Must start with http:// or https:// and contain a valid domain")
    private String url;
    
    @Builder.Default
    private String analysisType = "basic";
    
    @Builder.Default
    private String deviceType = "desktop"; // Cihaz tipi alanı ekle (varsayılan: desktop)
    
    private Map<String, Object> options;
}
