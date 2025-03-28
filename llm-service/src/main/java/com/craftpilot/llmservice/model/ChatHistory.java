package com.craftpilot.llmservice.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.DocumentId;
import com.craftpilot.llmservice.util.TimestampDeserializer;
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
public class ChatHistory {
    @DocumentId
    private String id;
    private String userId;
    private String title;
    
    @JsonDeserialize(using = TimestampDeserializer.class)
    private Timestamp createdAt;
    
    @JsonDeserialize(using = TimestampDeserializer.class)
    private Timestamp updatedAt;
    
    @Builder.Default
    private List<Conversation> conversations = new ArrayList<>();
    private String aiModel;
    private double temperature;
    
    private String lastConversation; // Son mesajı önizleme için saklamak üzere eklenen alan
    
    @Builder.Default
    private boolean enable = true;
}
