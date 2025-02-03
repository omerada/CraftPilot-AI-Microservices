package com.craftpilot.aiquestionservice.service.web;

import com.craftpilot.aiquestionservice.model.SearchResult; 
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import org.springframework.beans.factory.annotation.Qualifier;

@Service
public class WebSearchService {
    private final WebClient webClient;

    @Value("${google.search.api.key}")
    private String apiKey;

    @Value("${google.search.engine.id}")
    private String searchEngineId;

    public WebSearchService(@Qualifier("geminiWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<SearchResult> search(String query) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/customsearch/v1")
                        .queryParam("key", apiKey)
                        .queryParam("cx", searchEngineId)
                        .queryParam("q", query)
                        .build())
                .retrieve()
                .bodyToMono(SearchResult.class);
    }
} 