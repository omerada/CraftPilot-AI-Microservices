package com.craftpilot.imageservice.service;

import com.theokanning.openai.image.CreateImageRequest;
import com.theokanning.openai.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class OpenAIService {
    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.model}")
    private String model;

    @Value("${openai.size}")
    private String size;

    @Value("${openai.quality}")
    private String quality;

    @Value("${openai.style}")
    private String style;

    private OpenAiService openAiService;

    private OpenAiService getOpenAiService() {
        if (openAiService == null) {
            openAiService = new OpenAiService(apiKey);
        }
        return openAiService;
    }

    public Mono<String> generateImage(String prompt) {
        return Mono.fromCallable(() -> {
            CreateImageRequest request = CreateImageRequest.builder()
                    .prompt(prompt)
                    .model(model)
                    .size(size)
                    .quality(quality)
                    .style(style)
                    .n(1)
                    .build();

            return getOpenAiService().createImage(request)
                    .getData().get(0).getUrl();
        });
    }
} 