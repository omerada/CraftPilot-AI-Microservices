package com.craftpilot.translationservice.service;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OpenAIService {
    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.model}")
    private String model;

    @Value("${openai.max-tokens}")
    private Integer maxTokens;

    @Value("${openai.temperature}")
    private Double temperature;

    private OpenAiService openAiService;

    private OpenAiService getOpenAiService() {
        if (openAiService == null) {
            openAiService = new OpenAiService(apiKey);
        }
        return openAiService;
    }

    public Mono<String> translate(String sourceText, String sourceLanguage, String targetLanguage) {
        return Mono.fromCallable(() -> {
            String systemPrompt = String.format(
                "You are a professional translator. Translate the following text from %s to %s. " +
                "Provide only the translated text without any additional comments or explanations.",
                sourceLanguage, targetLanguage
            );

            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(List.of(
                            new ChatMessage("system", systemPrompt),
                            new ChatMessage("user", sourceText)
                    ))
                    .maxTokens(maxTokens)
                    .temperature(temperature)
                    .build();

            return getOpenAiService().createChatCompletion(request)
                    .getChoices().get(0).getMessage().getContent();
        });
    }
} 