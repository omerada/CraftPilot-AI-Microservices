package com.craftpilot.llmservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
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
    private List<Map<String, Object>> messages;  
    private Integer maxTokens;
    private Double temperature;
    private String requestType;
    private String language;  
    private Boolean stream;  
    private String context;
    
    // Bu alanları geri ekleyelim ancak mesajlar listesi ile entegre çalışacaklar
    private String prompt;
    private String systemPrompt;
    
    /**
     * Returns the user prompt from the request.
     * If the prompt field is set, it returns that value.
     * Otherwise, it tries to extract the last user message from the messages list.
     * @return The user prompt or null if not found
     */
    public String getPrompt() {
        if (prompt != null && !prompt.isEmpty()) {
            return prompt;
        }
        
        if (messages != null && !messages.isEmpty()) {
            // Find the last user message
            for (int i = messages.size() - 1; i >= 0; i--) {
                Map<String, Object> message = messages.get(i);
                if (message.containsKey("role") && "user".equals(message.get("role")) && 
                    message.containsKey("content")) {
                    Object content = message.get("content");
                    if (content != null) {
                        return content.toString();
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * Sets the prompt and also updates the messages list to include this prompt
     * @param prompt The user prompt
     */
    public void setPrompt(String prompt) {
        this.prompt = prompt;
        
        // Also update the messages list
        if (prompt != null && !prompt.isEmpty()) {
            if (messages == null) {
                messages = new ArrayList<>();
            }
            
            // Check if there's already a user message
            boolean userMessageFound = false;
            for (int i = messages.size() - 1; i >= 0; i--) {
                Map<String, Object> message = messages.get(i);
                if (message.containsKey("role") && "user".equals(message.get("role"))) {
                    // Update existing user message
                    message.put("content", prompt);
                    userMessageFound = true;
                    break;
                }
            }
            
            // If no user message found, add a new one
            if (!userMessageFound) {
                Map<String, Object> userMessage = new HashMap<>();
                userMessage.put("role", "user");
                userMessage.put("content", prompt);
                messages.add(userMessage);
            }
        }
    }
    
    /**
     * Returns the system prompt from the request.
     * If the systemPrompt field is set, it returns that value.
     * Otherwise, it tries to extract the system message from the messages list.
     * @return The system prompt or null if not found
     */
    public String getSystemPrompt() {
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            return systemPrompt;
        }
        
        if (messages != null && !messages.isEmpty()) {
            // Find the system message
            for (Map<String, Object> message : messages) {
                if (message.containsKey("role") && "system".equals(message.get("role")) && 
                    message.containsKey("content")) {
                    Object content = message.get("content");
                    if (content != null) {
                        return content.toString();
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * Sets the system prompt and also updates the messages list to include this system prompt
     * @param systemPrompt The system prompt
     */
    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
        
        // Also update the messages list
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            if (messages == null) {
                messages = new ArrayList<>();
            }
            
            // Check if there's already a system message
            boolean systemMessageFound = false;
            for (Map<String, Object> message : messages) {
                if (message.containsKey("role") && "system".equals(message.get("role"))) {
                    // Update existing system message
                    message.put("content", systemPrompt);
                    systemMessageFound = true;
                    break;
                }
            }
            
            // If no system message found, add a new one at the beginning
            if (!systemMessageFound) {
                Map<String, Object> systemMessage = new HashMap<>();
                systemMessage.put("role", "system");
                systemMessage.put("content", systemPrompt);
                messages.add(0, systemMessage);
            }
        }
    }
}