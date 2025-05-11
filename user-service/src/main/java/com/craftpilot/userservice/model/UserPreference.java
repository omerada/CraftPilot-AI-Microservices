package com.craftpilot.userservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "userPreferences")
public class UserPreference {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String userId;
    
    private String language;
    private String theme;
    private String themeSchema;
    private String layout;
    private Map<String, Boolean> notifications;
    private Boolean pushEnabled;
    private List<String> aiModelFavorites;
    private String lastSelectedModelId;
    
    private Long createdAt;
    private Long updatedAt;
    
    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }
    
    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Long getCreatedAt() {
        return this.createdAt;
    }
    
    public Long getUpdatedAt() {
        return this.updatedAt;
    }
}