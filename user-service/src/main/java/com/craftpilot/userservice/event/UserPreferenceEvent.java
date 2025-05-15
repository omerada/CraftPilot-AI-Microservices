package com.craftpilot.userservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferenceEvent {
    private String userId;
    private String eventType;
    private Long timestamp;
    private String theme;
    private String language;
    private Boolean pushEnabled;
    
    // İhtiyaca göre ek alanlar eklenebilir
}
