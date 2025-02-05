package com.craftpilot.userservice.event;

import com.craftpilot.userservice.model.user.entity.UserEntity;
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
    private String action;
    private long timestamp;
    
    public static UserEvent fromUser(String eventType, UserEntity user) {
        return UserEvent.builder()
                .userId(user.getId())
                .action(eventType)
                .timestamp(System.currentTimeMillis())
                .build();
    }
} 