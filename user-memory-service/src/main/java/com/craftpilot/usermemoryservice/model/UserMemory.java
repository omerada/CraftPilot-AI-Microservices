package com.craftpilot.usermemoryservice.model;

import com.google.cloud.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserMemory {
    private String id; // userId will be used as document ID
    private String userId;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    
    @Builder.Default
    private List<MemoryEntry> entries = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemoryEntry {
        private String id;
        private String content;
        private String source;
        private Timestamp timestamp;
        private Double importance; // 0.0 to 1.0 representing importance
        private String category;
        private List<String> tags;
    }
}
