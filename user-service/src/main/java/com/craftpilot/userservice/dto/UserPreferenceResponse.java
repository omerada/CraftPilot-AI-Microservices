package com.craftpilot.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferenceResponse {
    private String userId;
    private String theme;
    private String language;
    private boolean notifications;
    private boolean pushEnabled;
    private Long createdAt;
    private Long updatedAt;
}
