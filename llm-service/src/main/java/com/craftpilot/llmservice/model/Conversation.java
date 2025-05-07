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
    private String role;
    private String content;
    private Boolean fresh;
    
    @JsonDeserialize(using = TimestampDeserializer.class)
    private Timestamp timestamp;
    
    // orderIndex konuşma sıralaması için kullanılır
    private Integer orderIndex;
    
    private Boolean isCanceled;

    public Boolean getIsCanceled() {
        return isCanceled;
    }
    
    public void setIsCanceled(Boolean isCanceled) {
        this.isCanceled = isCanceled;
    }
}
