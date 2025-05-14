package com.craftpilot.usermemoryservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "response_preferences")
public class ResponsePreference {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String userId;
    
    private String formality;
    private String verbosity;
    private String tone;
    private String language;
    private String communicationStyle; // Added this field
    
    @Builder.Default
    private List<String> customStyles = new ArrayList<>();
    
    @Builder.Default
    private Map<String, Object> additionalPreferences = new HashMap<>(); // Added this field
    
    @CreatedDate
    private LocalDateTime created; // Changed from createdAt
    
    @LastModifiedDate
    private LocalDateTime lastUpdated; // Changed from updatedAt
}
