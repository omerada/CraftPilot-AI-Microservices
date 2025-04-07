package com.craftpilot.llmservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIRequest {
    private String requestId;
    private String userId;
    private String model;
    private String prompt; // Geriye dönük uyumluluk için korundu
    private List<Map<String, Object>> messages; // Sohbet geçmişi için
    private Integer maxTokens;
    private Double temperature;
    private String requestType;
    private String language; // Kullanıcı dil tercihi
    private String systemPrompt;
}