package com.craftpilot.codeservice.service;

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

    public Mono<String> generateCode(String prompt, String language, String framework) {
        return Mono.fromCallable(() -> {
            String systemPrompt = String.format(
                "You are an expert software developer. Generate clean, efficient, and well-documented code in %s" +
                (framework != null ? " using the %s framework" : "") +
                ". Follow these guidelines:\n" +
                "1. Write production-quality code\n" +
                "2. Include necessary imports and dependencies\n" +
                "3. Add clear comments explaining complex logic\n" +
                "4. Follow best practices and design patterns\n" +
                "5. Consider performance and scalability\n" +
                "6. Handle errors appropriately\n\n" +
                "Provide only the code without any additional explanations.",
                language, framework
            );

            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(List.of(
                            new ChatMessage("system", systemPrompt),
                            new ChatMessage("user", prompt)
                    ))
                    .maxTokens(maxTokens)
                    .temperature(temperature)
                    .build();

            return getOpenAiService().createChatCompletion(request)
                    .getChoices().get(0).getMessage().getContent();
        });
    }
} 