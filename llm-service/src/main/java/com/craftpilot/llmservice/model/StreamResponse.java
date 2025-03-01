package com.craftpilot.llmservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Stream yanıtları için basit bir veri transfer nesnesi.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreamResponse {
    private String content;
    private boolean done;
}
