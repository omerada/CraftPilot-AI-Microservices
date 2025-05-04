package com.craftpilot.usermemoryservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractionResponse {
    private String userId;
    private List<InformationItem> informationItems;
}
