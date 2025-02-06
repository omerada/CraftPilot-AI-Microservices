package com.craftpilot.userservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferenceResponse {
    private String theme;
    private String language;
    private boolean notifications;
}