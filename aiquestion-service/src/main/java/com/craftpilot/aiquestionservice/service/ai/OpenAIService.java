package com.craftpilot.aiquestionservice.service.ai;

import com.craftpilot.aiquestionservice.model.AIModel;
import com.craftpilot.aiquestionservice.model.Question;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAIService {

    public Mono<String> processQuestion(Question question, AIModel model) {
        OpenAiService service = new OpenAiService(model.getApiKey());

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system", buildSystemPrompt(question.getPreferences())));
        messages.add(new ChatMessage("user", buildUserPrompt(question)));

        ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
                .model(model.getVersion())
                .messages(messages)
                .maxTokens(model.getMaxTokens())
                .temperature(model.getTemperature())
                .build();

        return Mono.fromCallable(() -> service.createChatCompletion(completionRequest).getChoices().get(0).getMessage().getContent())
                .doOnSuccess(response -> log.info("Successfully processed question with OpenAI"))
                .doOnError(error -> log.error("Error processing question with OpenAI: {}", error.getMessage()));
    }

    private String buildSystemPrompt(Map<String, Object> preferences) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an AI assistant specialized in ");
        prompt.append(preferences.getOrDefault("domain", "general knowledge"));
        prompt.append(". Please provide responses in a ");
        prompt.append(preferences.getOrDefault("responseStyle", "professional"));
        prompt.append(" style using ");
        prompt.append(preferences.getOrDefault("language", "English"));
        prompt.append(" language. Your responses should be ");
        prompt.append(preferences.getOrDefault("responseStyle", "concise"));
        prompt.append(".");
        return prompt.toString();
    }

    private String buildUserPrompt(Question question) {
        StringBuilder prompt = new StringBuilder();
        if (question.getContext() != null && !question.getContext().isEmpty()) {
            prompt.append("Context: ").append(question.getContext()).append("\n\n");
        }
        prompt.append("Question: ").append(question.getQuestion());
        return prompt.toString();
    }
} 