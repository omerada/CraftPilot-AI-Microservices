package com.craftpilot.llmservice.model.performance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreamSuggestionsResponse {
    private String requestId;
    private String content;
    private boolean done;
    private boolean error;
    private boolean ping;
}
