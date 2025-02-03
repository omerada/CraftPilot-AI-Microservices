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
    private String userId;
    private Long timestamp;
    private UserEntity user;
    
    public static UserEvent fromUser(String eventType, UserEntity user) {
        return UserEvent.builder()
                .eventType(eventType)
                .userId(user.getId())
                .timestamp(System.currentTimeMillis())
                .user(user)
                .build();
    }
} 