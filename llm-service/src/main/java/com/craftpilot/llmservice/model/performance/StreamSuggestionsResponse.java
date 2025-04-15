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
    
    public static StreamSuggestionsResponse error(String errorMessage) {
        return StreamSuggestionsResponse.builder()
                .content(errorMessage)
                .error(true)
                .done(true)
                .build();
    }
    
    public static StreamSuggestionsResponse content(String content, boolean done) {
        return StreamSuggestionsResponse.builder()
                .content(content)
                .done(done)
                .error(false)
                .build();
    }
    
    public static StreamSuggestionsResponse ping() {
        return StreamSuggestionsResponse.builder()
                .content("")
                .ping(true)
                .done(false)
                .error(false)
                .build();
    }
}
