package com.craftpilot.llmservice.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "chat_histories")
public class ChatHistory {
    @Id
    private String id;

    @Indexed
    private String userId;

    private String title;

    @Indexed
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Builder.Default
    private List<Conversation> conversations = new ArrayList<>();
    private String aiModel;
    private double temperature;

    private String lastConversation; // Son mesajı önizleme için saklamak üzere eklenen alan

    @Indexed
    private String sessionId;

    @Builder.Default
    private boolean enable = true;
}
