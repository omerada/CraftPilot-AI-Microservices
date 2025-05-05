package com.craftpilot.usermemoryservice.dto;

import com.craftpilot.usermemoryservice.model.UserInstruction;
import com.craftpilot.usermemoryservice.model.ResponsePreference;
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
public class ContextResponse {
    private String userId;
    
    @Builder.Default
    private Map<String, List<Map<String, Object>>> extractedMemories = new HashMap<>();
    
    @Builder.Default
    private List<UserInstruction> customInstructions = new ArrayList<>();
    
    private ResponsePreference responsePreferences;
    
    @Builder.Default
    private String formattedContext = "";
}
