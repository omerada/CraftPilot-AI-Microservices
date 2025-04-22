package com.craftpilot.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferenceRequest {
    private String theme;
    private String language;
    private String themeSchema;
    private String layout;
    private Map<String, Boolean> notifications;
    private Boolean pushEnabled;
    private List<String> aiModelFavorites;
    private String lastSelectedModelId;
}
