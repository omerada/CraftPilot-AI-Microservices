package com.craftpilot.userservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEvent {
    private String userId;
    private String eventType;
    private long timestamp;
    // Add other necessary fields
    
    public static UserEvent fromUser(String eventType, String userId) {
        return UserEvent.builder()
                .eventType(eventType)
                .userId(userId)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}