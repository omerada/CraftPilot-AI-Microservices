package com.craftpilot.usermemoryservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponsePreference {
    private String id;
    private String userId;
    private String language;
    private String communicationStyle;
    
    @Builder.Default
    private Map<String, Object> additionalPreferences = new HashMap<>();
    
    @Builder.Default
    private LocalDateTime created = LocalDateTime.now();
    
    @Builder.Default
    private LocalDateTime lastUpdated = LocalDateTime.now();
}
