package com.craftpilot.codeservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeHistory {
    private String id;
    private String userId;
    private List<Code> codes;
    private Instant createdAt;
    private Instant updatedAt;
} 