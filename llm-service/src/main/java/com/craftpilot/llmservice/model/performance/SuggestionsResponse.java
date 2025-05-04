package com.craftpilot.llmservice.model.performance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuggestionsResponse {
    private String content;
    private boolean success;
    private String error;
}
