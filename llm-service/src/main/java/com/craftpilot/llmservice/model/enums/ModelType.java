package com.craftpilot.llmservice.model.enums;

public enum ModelType {
    GPT_4("openai/gpt-4"),
    GPT_4_TURBO("openai/gpt-4-turbo-preview"),
    GPT_3_5_TURBO("openai/gpt-3.5-turbo"),
    CLAUDE_3_OPUS("anthropic/claude-3-opus"),
    CLAUDE_3_SONNET("anthropic/claude-3-sonnet"),
    CLAUDE_3_HAIKU("anthropic/claude-3-haiku"),
    MISTRAL_MEDIUM("mistral/mistral-medium"),
    MISTRAL_SMALL("mistral/mistral-small"),
    MIXTRAL_8X7B("mistral/mixtral-8x7b");

    private final String modelId;

    ModelType(String modelId) {
        this.modelId = modelId;
    }

    public String getModelId() {
        return modelId;
    }
} 