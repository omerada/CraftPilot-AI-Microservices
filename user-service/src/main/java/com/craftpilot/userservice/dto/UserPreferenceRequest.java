package com.craftpilot.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferenceRequest {
    private String theme;
    private String language;
    private Boolean notifications;
    private Boolean pushEnabled;
    private List<String> aiModelFavorites;
}
