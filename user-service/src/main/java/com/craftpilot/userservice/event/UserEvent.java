package com.craftpilot.userservice.event;

import com.craftpilot.userservice.model.user.entity.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEvent {
    private String eventId;
    private String userId;
    private String eventType;
    private LocalDateTime timestamp;
    private UserEntity user;
    private String error;

    public static UserEvent fromUser(String eventType, UserEntity user) {
        return UserEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .userId(user.getId())
                .eventType(eventType)
                .timestamp(LocalDateTime.now())
                .user(user)
                .build();
    }

    public static UserEvent error(UserEntity user, String error) {
        return UserEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .userId(user.getId())
                .eventType("USER_ERROR")
                .timestamp(LocalDateTime.now())
                .user(user)
                .error(error)
                .build();
    }
}