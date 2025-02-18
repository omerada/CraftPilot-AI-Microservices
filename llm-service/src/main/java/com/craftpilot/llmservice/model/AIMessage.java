package com.craftpilot.llmservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIMessage {
    private String role;
    private List<AIMessageContent> content;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class AIMessageContent {
    private String type;
    private String text;
    private ImageUrl image_url;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class ImageUrl {
    private String url;
}
