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
public class UserInstruction {
    private String id;
    private String userId;
    private String content;
    private Integer priority;
    private String category;
    
    @Builder.Default
    private LocalDateTime created = LocalDateTime.now();
    
    @Builder.Default
    private LocalDateTime lastUpdated = LocalDateTime.now();
}
