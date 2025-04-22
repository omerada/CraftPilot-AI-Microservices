package com.craftpilot.userservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import com.google.cloud.firestore.annotation.DocumentId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TypeAlias("userPreference")
public class UserPreference {
    @Id
    @DocumentId
    private String userId;

    // Tema modu (light, dark, system)
    @Builder.Default
    private String theme = "system";

    // Tema şeması (default, dark, green, purple, orange)
    @Builder.Default
    private String themeSchema = "default";

    // Kullanıcının tercih ettiği dil
    @Builder.Default
    private String language = "en";

    // Layout tercihi (collapsibleSide, framelessSide)
    @Builder.Default
    private String layout = "collapsibleSide";

    // Kullanıcının favori AI modelleri
    @Builder.Default
    private List<String> aiModelFavorites = new ArrayList<>();

    // Bildirim tercihleri
    @Builder.Default
    private Map<String, Boolean> notifications = new HashMap<>();
    
    @Builder.Default
    private Boolean pushEnabled = false;

    // Zaman damgaları
    private Long createdAt;
    private Long updatedAt;
}