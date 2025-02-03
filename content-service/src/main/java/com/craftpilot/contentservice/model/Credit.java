package com.craftpilot.contentservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Credit {
    private String id;
    private String userId;
    private Integer credits;
    private Instant createdAt;
    private Instant updatedAt;
} 