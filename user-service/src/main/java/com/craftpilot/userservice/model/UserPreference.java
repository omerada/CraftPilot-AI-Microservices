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
@RedisHash("user_preference")
public class UserPreference implements Serializable {
    @Id
    private String userId;
    private String theme;
    private String language;
    private boolean notifications;  // Changed from emailNotifications
    private boolean pushEnabled;    // Changed from pushNotifications
}