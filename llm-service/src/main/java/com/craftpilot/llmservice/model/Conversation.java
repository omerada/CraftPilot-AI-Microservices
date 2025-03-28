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
    private String content;
    private String role; // 'user' veya 'assistant'
    
    @JsonDeserialize(using = TimestampDeserializer.class)
    private Timestamp timestamp;
    
    private boolean fresh;
}
