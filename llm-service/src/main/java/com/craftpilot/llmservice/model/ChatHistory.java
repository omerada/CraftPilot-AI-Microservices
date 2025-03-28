package com.craftpilot.llmservice.model;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.DocumentId;
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
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private List<Conversation> conversations = new ArrayList<>();
    private String aiModel;
    private double temperature;
}
