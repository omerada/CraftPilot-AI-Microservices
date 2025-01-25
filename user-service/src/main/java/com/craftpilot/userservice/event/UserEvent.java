package com.craftpilot.userservice.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import java.time.LocalDateTime;

@Getter
@ToString
@RequiredArgsConstructor
public class UserEvent {
    private final String uid;
    private final String email;
    private final UserEventType type;
    private final LocalDateTime timestamp = LocalDateTime.now();

    public enum UserEventType {
        UPDATED,
        PROFILE_VIEWED
    }
} 