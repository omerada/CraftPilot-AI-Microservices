package com.craftpilot.usermemoryservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemoryEntryRequest {
    private String content;
    private String source;
    private String context;
    private Map<String, Object> metadata;
    private LocalDateTime timestamp;
    private Double importance;
}
