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

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "user_instructions")
public class UserInstruction {
    
    @Id
    private String id;
    
    @Indexed
    private String userId;
    
    private String content;
    private String category;
    private Integer priority;
    private Boolean active;
    
    @CreatedDate
    private LocalDateTime created; 
    
    @LastModifiedDate
    private LocalDateTime lastUpdated;
}
