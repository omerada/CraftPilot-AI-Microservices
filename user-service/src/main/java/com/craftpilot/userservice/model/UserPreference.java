package com.craftpilot.userservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("user_preferences")
public class UserPreference implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    private String userId;
    private boolean notifications;
    private String theme;
    private String language;
    private boolean emailNotifications;
    private boolean pushNotifications;
    // Add other preference fields as needed
}