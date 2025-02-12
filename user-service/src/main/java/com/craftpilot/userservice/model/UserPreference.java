package com.craftpilot.userservice.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;

@Data
@RedisHash("user_preference")
public class UserPreference implements Serializable {
    @Id
    private String userId;
    private String theme;
    private String language;
    private boolean emailNotifications;
    private boolean pushNotifications;
}