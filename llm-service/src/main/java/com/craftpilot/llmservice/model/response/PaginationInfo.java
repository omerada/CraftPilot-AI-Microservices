package com.craftpilot.llmservice.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginationInfo {
    private int currentPage;
    private int totalPages;
    private int pageSize;
    private int totalItems;
    private boolean hasMore;
}
