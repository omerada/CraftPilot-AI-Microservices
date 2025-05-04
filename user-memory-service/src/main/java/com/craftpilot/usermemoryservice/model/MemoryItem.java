package com.craftpilot.usermemoryservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryItem {
    private String id;
    private String content;
    private String source;
    private double importance;
    
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Builder.Default
    private boolean active = true;
    
    @Builder.Default
    private int usageCount = 0;
    
    private String category;
}
