package com.craftpilot.userservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserPreferenceRequest {
    @NotNull
    private String theme;
    @NotNull
    private String language;
    private boolean notifications;
    private boolean pushEnabled;
}
