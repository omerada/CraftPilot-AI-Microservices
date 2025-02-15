package com.craftpilot.userservice.model.user.event;

import com.craftpilot.userservice.model.user.User;
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
    private long timestamp;
    private User user;

    public static UserEvent fromEntity(UserEntity entity, String eventType) {
        return UserEvent.builder()
                .eventType(eventType)
                .userId(entity.getId())
                .timestamp(System.currentTimeMillis())
                .user(User.fromEntity(entity))
                .build();
    }
}
