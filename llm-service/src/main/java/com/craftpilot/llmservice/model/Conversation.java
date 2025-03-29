package com.craftpilot.llmservice.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.cloud.Timestamp;
import com.craftpilot.llmservice.util.TimestampDeserializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {
    private String id;
    private String role;
    private String content;
    private Boolean fresh;
    
    @JsonDeserialize(using = TimestampDeserializer.class)
    private Timestamp timestamp;
    
    // Replace sequence with orderIndex
    private Integer orderIndex;
    
    // Keep sequence field for backward compatibility but mark as deprecated
    @Deprecated
    private Long sequence;
}
