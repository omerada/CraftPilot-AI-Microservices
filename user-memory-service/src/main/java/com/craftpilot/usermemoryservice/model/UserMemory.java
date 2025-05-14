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
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "user_memories")
public class UserMemory {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String userId;
    
    @CreatedDate
    private LocalDateTime created;
    
    @LastModifiedDate
    private LocalDateTime lastUpdated;
    
    @Builder.Default
    private List<MemoryItem> entries = new ArrayList<>();
}
