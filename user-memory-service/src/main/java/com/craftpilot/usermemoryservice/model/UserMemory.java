package com.craftpilot.usermemoryservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserMemory {
    private String id; // userId will be used as document ID
    private String userId;
    private LocalDateTime created;
    private LocalDateTime lastUpdated;
    
    @Builder.Default
    private List<Map<String, Object>> entries = new ArrayList<>();
}
