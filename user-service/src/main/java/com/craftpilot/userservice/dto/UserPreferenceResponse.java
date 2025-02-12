package com.craftpilot.userservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserPreferenceResponse {
    private String userId;
    private String theme;
    private String language;
    private boolean notifications;
    private boolean pushEnabled;
}
