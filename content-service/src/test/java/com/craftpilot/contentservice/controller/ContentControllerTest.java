package com.craftpilot.contentservice.controller;

import com.craftpilot.contentservice.model.Content;
import com.craftpilot.contentservice.model.ContentType;
import com.craftpilot.contentservice.model.dto.ContentRequest;
import com.craftpilot.contentservice.model.dto.ContentResponse;
import com.craftpilot.contentservice.service.ContentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebFluxTest(ContentController.class)
class ContentControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ContentService contentService;

    private Content content;
    private ContentRequest contentRequest;
    private ContentResponse contentResponse;

    @BeforeEach
    void setUp() {
        content = Content.builder()
                .id("1")
                .userId("user1")
                .title("Test Title")
                .description("Test Description")
                .content("Test Content")
                .type(ContentType.TEXT)
                .tags(Arrays.asList("test", "sample"))
                .metadata(new HashMap<>())
                .build();

        contentRequest = ContentRequest.builder()
                .title("Test Title")
                .description("Test Description")
                .content("Test Content")
                .type(ContentType.TEXT)
                .tags(Arrays.asList("test", "sample"))
                .metadata(new HashMap<>())
                .build();

        contentResponse = ContentResponse.builder()
                .id(content.getId())
                .userId(content.getUserId())
                .title(content.getTitle())
                .description(content.getDescription())
                .content(content.getContent())
                .type(content.getType())
                .tags(content.getTags())
                .metadata(content.getMetadata())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @WithMockUser(username = "user1")
    void createContent_Success() {
        when(contentService.createContent(eq("user1"), any(ContentRequest.class)))
                .thenReturn(Mono.just(content));

        webTestClient.post()
                .uri("/contents")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(contentRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo(contentResponse.getId())
                .jsonPath("$.userId").isEqualTo(contentResponse.getUserId())
                .jsonPath("$.title").isEqualTo(contentResponse.getTitle());
    }

    @Test
    @WithMockUser(username = "user1")
    void getContent_Success() {
        when(contentService.getContent("1"))
                .thenReturn(Mono.just(content));

        webTestClient.get()
                .uri("/contents/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(contentResponse.getId())
                .jsonPath("$.title").isEqualTo(contentResponse.getTitle());
    }

    @Test
    @WithMockUser(username = "user1")
    void getAllContents_Success() {
        when(contentService.getAllContents())
                .thenReturn(Flux.just(content));

        webTestClient.get()
                .uri("/contents")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ContentResponse.class);
    }

    @Test
    @WithMockUser(username = "user1")
    void getContentsByType_Success() {
        when(contentService.getContentsByType(ContentType.TEXT.name()))
                .thenReturn(Flux.just(content));

        webTestClient.get()
                .uri("/contents/type/TEXT")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ContentResponse.class);
    }

    @Test
    @WithMockUser(username = "user1")
    void searchContents_Success() {
        when(contentService.searchContents("test"))
                .thenReturn(Flux.just(content));

        webTestClient.get()
                .uri("/contents/search?query=test")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ContentResponse.class);
    }

    @Test
    @WithMockUser(username = "user1")
    void updateContent_Success() {
        when(contentService.updateContent(eq("1"), eq("user1"), any(ContentRequest.class)))
                .thenReturn(Mono.just(content));

        webTestClient.put()
                .uri("/contents/1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(contentRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ContentResponse.class);
    }

    @Test
    @WithMockUser(username = "user1")
    void deleteContent_Success() {
        when(contentService.deleteContent("1", "user1"))
                .thenReturn(Mono.empty());

        webTestClient.delete()
                .uri("/contents/1")
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    @WithMockUser(username = "user1")
    void publishContent_Success() {
        when(contentService.publishContent("1", "user1"))
                .thenReturn(Mono.just(content));

        webTestClient.post()
                .uri("/contents/1/publish")
                .exchange()
                .expectStatus().isOk()
                .expectBody(ContentResponse.class);
    }

    @Test
    @WithMockUser(username = "user1")
    void archiveContent_Success() {
        when(contentService.archiveContent("1", "user1"))
                .thenReturn(Mono.just(content));

        webTestClient.post()
                .uri("/contents/1/archive")
                .exchange()
                .expectStatus().isOk()
                .expectBody(ContentResponse.class);
    }

    @Test
    @WithMockUser(username = "user1")
    void improveContent_Success() {
        when(contentService.improveContent("1", "user1"))
                .thenReturn(Mono.just(content));

        webTestClient.post()
                .uri("/contents/1/improve")
                .exchange()
                .expectStatus().isOk()
                .expectBody(ContentResponse.class);
    }
} 