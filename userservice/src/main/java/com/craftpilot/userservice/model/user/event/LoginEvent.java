package com.craftpilot.userservice.model.user.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Event representing a successful user login operation.
 */
@Getter
@AllArgsConstructor
public class LoginEvent {
    private final String userId;
    private final String userEmail;
    private final String accessToken;
}
