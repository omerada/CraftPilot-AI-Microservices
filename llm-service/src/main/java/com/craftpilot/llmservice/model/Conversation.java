package com.craftpilot.llmservice.model;

import com.google.cloud.Timestamp;
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
    private Timestamp timestamp;
    private boolean fresh;
}
