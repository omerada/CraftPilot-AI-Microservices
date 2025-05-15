package com.craftpilot.userservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "user_preferences")
public class UserPreference {
    @Id
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

    // Son seçilen AI modeli ID'si
    @Builder.Default
    private String lastSelectedModelId = "google/gemini-2.0-flash-lite-001";

    // Zaman damgaları
    private Long createdAt;
    private Long updatedAt;
    
    // Varsayılan tercihler oluşturma yardımcı metodu
    public static UserPreference createDefaultPreference(String userId) {
        Map<String, Boolean> notificationsMap = new HashMap<>();
        notificationsMap.put("general", true);
        
        return UserPreference.builder()
                .userId(userId)
                .theme("light")
                .language("tr")
                .themeSchema("default")
                .layout("collapsibleSide")
                .notifications(notificationsMap)
                .pushEnabled(true)
                .aiModelFavorites(new ArrayList<>())
                .lastSelectedModelId("google/gemini-2.0-flash-lite-001")
                .createdAt(System.currentTimeMillis())
                .updatedAt(System.currentTimeMillis())
                .build();
    }
}