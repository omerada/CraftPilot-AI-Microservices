package com.craftpilot.contentservice.service.ai;

import com.craftpilot.contentservice.model.Content;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OpenAIService implements AIModelService {

    @Value("${openai.api-key}")
    private String apiKey;

    private OpenAiService openAiService;

    private OpenAiService getOpenAiService() {
        if (openAiService == null) {
            openAiService = new OpenAiService(apiKey);
        }
        return openAiService;
    }

    @Override
    public Mono<String> generateContent(Content content) {
        return Mono.fromCallable(() -> {
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model("gpt-4")
                    .messages(Arrays.asList(
                            new ChatMessage("system", "You are a helpful content generation assistant."),
                            new ChatMessage("user", content.getContent())
                    ))
                    .temperature(0.7)
                    .maxTokens(2000)
                    .build();

            return getOpenAiService().createChatCompletion(request)
                    .getChoices().get(0).getMessage().getContent();
        });
    }

    @Override
    public Mono<String> improveContent(Content content) {
        return Mono.fromCallable(() -> {
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model("gpt-4")
                    .messages(Arrays.asList(
                            new ChatMessage("system", "You are a content improvement assistant. Improve the following content while maintaining its core message and style."),
                            new ChatMessage("user", content.getContent())
                    ))
                    .temperature(0.7)
                    .maxTokens(2000)
                    .build();

            return getOpenAiService().createChatCompletion(request)
                    .getChoices().get(0).getMessage().getContent();
        });
    }

    @Override
    public Mono<String> analyzeContent(Content content) {
        return Mono.fromCallable(() -> {
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model("gpt-4")
                    .messages(Arrays.asList(
                            new ChatMessage("system", "You are a content analysis assistant. Analyze the following content and provide insights about its quality, readability, and potential improvements."),
                            new ChatMessage("user", content.getContent())
                    ))
                    .temperature(0.7)
                    .maxTokens(1000)
                    .build();

            return getOpenAiService().createChatCompletion(request)
                    .getChoices().get(0).getMessage().getContent();
        });
    }

    @Override
    public Mono<List<String>> generateSuggestions(Content content) {
        return Mono.fromCallable(() -> {
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model("gpt-4")
                    .messages(Arrays.asList(
                            new ChatMessage("system", "Generate related content suggestions."),
                            new ChatMessage("user", "Generate 3 related content suggestions for: " + content.getContent())
                    ))
                    .temperature(0.8)
                    .maxTokens(1000)
                    .build();

            String response = getOpenAiService().createChatCompletion(request)
                    .getChoices().get(0).getMessage().getContent();

            return Arrays.asList(response.split("\n"));
        });
    }

    @Override
    public String getModelName() {
        return "gpt-4";
    }
} 