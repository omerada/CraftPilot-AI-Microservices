package com.craftpilot.usermemoryservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemoryItem {
    
    private String content;
    private String source;
    private String context;
    
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
    
    private LocalDateTime timestamp;
    private Double importance;
}
