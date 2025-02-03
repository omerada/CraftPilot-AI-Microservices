package com.craftpilot.contentservice.service.ai;

import com.craftpilot.contentservice.model.Content;
import com.craftpilot.contentservice.model.ContentType;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import com.theokanning.openai.service.OpenAiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils; 
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenAIServiceTest {

    @Mock
    private OpenAiService openAiService;

    @InjectMocks
    private OpenAIService service;

    private Content content;
    private ChatCompletionResult chatCompletionResult;

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
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        ChatMessage responseMessage = new ChatMessage("assistant", "Generated content");

        chatCompletionResult = new ChatCompletionResult();
        ChatCompletionChoice choice = new ChatCompletionChoice();
        choice.setIndex(0);
        choice.setMessage(responseMessage);
        choice.setFinishReason("stop");
        chatCompletionResult.setChoices(List.of(choice));

        ReflectionTestUtils.setField(service, "model", "gpt-4");
        ReflectionTestUtils.setField(service, "openAiService", openAiService);
    }

    @Test
    void generateContent_Success() {
        when(openAiService.createChatCompletion(any(ChatCompletionRequest.class)))
                .thenReturn(chatCompletionResult);

        StepVerifier.create(service.generateContent(content))
                .expectNext("Generated content")
                .verifyComplete();
    }

    @Test
    void improveContent_Success() {
        when(openAiService.createChatCompletion(any(ChatCompletionRequest.class)))
                .thenReturn(chatCompletionResult);

        StepVerifier.create(service.improveContent(content))
                .expectNext("Generated content")
                .verifyComplete();
    }

    @Test
    void analyzeContent_Success() {
        when(openAiService.createChatCompletion(any(ChatCompletionRequest.class)))
                .thenReturn(chatCompletionResult);

        StepVerifier.create(service.analyzeContent(content))
                .expectNext("Generated content")
                .verifyComplete();
    }

    @Test
    void generateSuggestions_Success() {
        when(openAiService.createChatCompletion(any(ChatCompletionRequest.class)))
                .thenReturn(chatCompletionResult);

        StepVerifier.create(service.generateSuggestions(content))
                .expectNext(Arrays.asList("Generated content"))
                .verifyComplete();
    }
} 