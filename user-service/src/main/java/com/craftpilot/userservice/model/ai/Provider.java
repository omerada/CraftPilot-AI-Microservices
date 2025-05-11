package com.craftpilot.userservice.model.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "providers")
public class Provider {

    @Id
    private String id;
    
    @Indexed(unique = true)
    private String name;
    
    private String displayName;
    private String baseUrl;
    private String authType;
    private String description;
    private String docUrl;
    private Map<String, Object> config;
    private String icon;
    private List<AIModel> models;
    
    @Builder.Default
    private boolean enabled = true;
    
    @Builder.Default
    private Instant createdAt = Instant.now();
    
    @Builder.Default
    private Instant updatedAt = Instant.now();
}
