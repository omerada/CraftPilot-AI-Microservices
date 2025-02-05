package com.craftpilot.codeservice.service;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class OpenAIService {

    private final OpenAiService openAiService;
    private static final String MODEL = "gpt-3.5-turbo";
    private static final int MAX_TOKENS = 2000;
    private static final double TEMPERATURE = 0.7;

    public OpenAIService(@Value("${openai.api.key}") String apiKey) {
        this.openAiService = new OpenAiService(apiKey, Duration.ofSeconds(60));
    }

    public Mono<String> generateCodeResponse(String prompt) {
        return Mono.fromCallable(() -> {
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage("system", "You are a helpful programming assistant."));
            messages.add(new ChatMessage("user", prompt));

            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(MODEL)
                    .messages(messages)
                    .maxTokens(MAX_TOKENS)
                    .temperature(TEMPERATURE)
                    .build();

            try {
                return openAiService.createChatCompletion(request)
                        .getChoices().get(0)
                        .getMessage().getContent();
            } catch (Exception e) {
                log.error("Error generating code response: ", e);
                throw e;
            }
        });
    }
} 