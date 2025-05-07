package com.craftpilot.usermemoryservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponsePreferenceRequest {
    
    @NotBlank(message = "Dil tercihi belirtilmelidir")
    private String language;
    
    @NotBlank(message = "İletişim stili belirtilmelidir")
    private String communicationStyle;
    
    @Builder.Default
    private Map<String, Object> additionalPreferences = new HashMap<>();
}
