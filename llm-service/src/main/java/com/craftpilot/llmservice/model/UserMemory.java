package com.craftpilot.llmservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserMemory {
    private String userId;
    
    @Builder.Default
    private List<MemoryEntry> memory = new ArrayList<>();
    
    private Instant lastUpdated;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemoryEntry {
        private Instant timestamp;
        private String information;
        private String context;
    }
}
