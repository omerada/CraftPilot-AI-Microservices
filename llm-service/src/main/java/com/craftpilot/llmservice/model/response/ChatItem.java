package com.craftpilot.llmservice.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatItem {
    private String id;
    private String title;
    private Object createdAt;
    private Object updatedAt;
    private String lastConversation;
}
