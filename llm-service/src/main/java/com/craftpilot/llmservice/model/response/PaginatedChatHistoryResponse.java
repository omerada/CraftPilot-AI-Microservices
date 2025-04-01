package com.craftpilot.llmservice.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginatedChatHistoryResponse {
    private LinkedHashMap<String, CategoryData> categories;
    private PaginationInfo pagination;
}
