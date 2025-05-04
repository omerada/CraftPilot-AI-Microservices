package com.craftpilot.llmservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractedUserInfo {
    private String userId;
    private String information;
    private String context;
    private Instant timestamp;
}
