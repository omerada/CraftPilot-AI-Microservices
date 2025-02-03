package com.craftpilot.aiquestionservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult {
    private List<SearchItem> items;
    private SearchInfo searchInfo;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchItem {
        private String title;
        private String link;
        private String snippet;
        private String url;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchInfo {
        private Double searchTime;
        private Long totalResults;
    }
} 