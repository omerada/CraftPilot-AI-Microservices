package com.craftpilot.userservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreference {
    private String userId;
    private String theme;
    private String language;
    private Boolean notifications;
    private Boolean pushEnabled;
    private List<String> aiModelFavorites; // Favori modeller için yeni alan
    private Long createdAt;
    private Long updatedAt;
}