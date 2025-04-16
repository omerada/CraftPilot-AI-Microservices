package com.craftpilot.lighthouseservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotEmpty;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisRequest {
    @NotEmpty(message = "URL cannot be empty")
    private String url;
    
    @Builder.Default
    private String analysisType = "basic"; // varsayılan olarak "basic", diğer değer "detailed"
    
    @Builder.Default
    private Map<String, Object> options = new HashMap<>();
    
    // getOptions metodunu override edelim
    public Map<String, Object> getOptions() {
        if (options == null) {
            options = new HashMap<>();
        }
        
        // analysisType değerini options içine ekle
        options.put("analysisType", analysisType);
        
        return options;
    }
}
