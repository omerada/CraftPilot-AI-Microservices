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
    private String eventType;
    private UserEntity user;
    private long timestamp;
    
    public static UserEvent fromUser(String eventType, UserEntity user) {
        return UserEvent.builder()
                .eventType(eventType)
                .user(user)
                .timestamp(System.currentTimeMillis())
                .build();
    }
} 