package com.craftpilot.usermemoryservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InformationItem {
    private String content;
    private String category;
    private double importance;
}
