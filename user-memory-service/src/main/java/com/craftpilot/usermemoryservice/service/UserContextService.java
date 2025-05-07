package com.craftpilot.usermemoryservice.service;

import com.craftpilot.usermemoryservice.dto.ContextResponse;
import com.craftpilot.usermemoryservice.model.ResponsePreference;
import com.craftpilot.usermemoryservice.model.UserInstruction;
import com.craftpilot.usermemoryservice.model.UserMemory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserContextService {
    private final UserMemoryService userMemoryService;
    private final UserInstructionService userInstructionService;
    private final ResponsePreferenceService responsePreferenceService;

    public Mono<ContextResponse> getConsolidatedContext(String userId, List<String> includeTags) {
        log.info("Getting consolidated context for userId: {} with tags: {}", userId, includeTags);
        
        Mono<UserMemory> memoryMono = userMemoryService.getUserMemory(userId);
        Mono<List<UserInstruction>> instructionsMono = userInstructionService.getUserInstructions(userId).collectList();
        Mono<ResponsePreference> preferenceMono = responsePreferenceService.getPreferences(userId);
        
        return Mono.zip(memoryMono, instructionsMono, preferenceMono)
            .map(tuple -> {
                UserMemory memory = tuple.getT1();
                List<UserInstruction> instructions = tuple.getT2();
                ResponsePreference preference = tuple.getT3();
                
                return buildContextResponse(userId, memory, instructions, preference, includeTags);
            })
            .switchIfEmpty(Mono.defer(() -> {
                // Eğer zip işlemi boş sonuç döndürürse (hiçbir veri yoksa)
                return Mono.just(ContextResponse.builder()
                    .userId(userId)
                    .formattedContext("Kullanıcı hakkında hiçbir bilgi bulunamadı.")
                    .build());
            }));
    }

    private ContextResponse buildContextResponse(
            String userId, 
            UserMemory memory, 
            List<UserInstruction> instructions, 
            ResponsePreference preference,
            List<String> includeTags) {
        
        ContextResponse response = ContextResponse.builder()
            .userId(userId)
            .build();
        
        // Belirtilen etiketlere göre içeriği filtrele veya hepsini ekle
        boolean includeMemories = includeTags == null || includeTags.isEmpty() || includeTags.contains("memories");
        boolean includeInstructions = includeTags == null || includeTags.isEmpty() || includeTags.contains("instructions");
        boolean includePreferences = includeTags == null || includeTags.isEmpty() || includeTags.contains("preferences");
        
        // Çıkarılmış bilgileri ekle
        if (includeMemories && memory != null && memory.getEntries() != null) {
            HashMap<String, List<Map<String, Object>>> groupedEntries = new HashMap<>();
            
            for (var entry : memory.getEntries()) {
                String context = entry.containsKey("context") ? (String) entry.get("context") : "general";
                if (!groupedEntries.containsKey(context)) {
                    groupedEntries.put(context, new ArrayList<>());
                }
                groupedEntries.get(context).add(entry);
            }
            
            response.setExtractedMemories(groupedEntries);
        }
        
        // Özel talimatları ekle
        if (includeInstructions) {
            // Talimatları önceliğe göre sırala
            List<UserInstruction> sortedInstructions = instructions.stream()
                .sorted(Comparator.comparing(UserInstruction::getPriority).reversed())
                .collect(Collectors.toList());
            
            response.setCustomInstructions(sortedInstructions);
        }
        
        // Dil ve stil tercihlerini ekle
        if (includePreferences && preference != null) {
            response.setResponsePreferences(preference);
        }
        
        // Formatlı bağlam oluştur
        response.setFormattedContext(formatContextForAI(response));
        
        return response;
    }

    private String formatContextForAI(ContextResponse response) {
        StringBuilder context = new StringBuilder();
        
        // Dil ve stil tercihlerini ekle
        if (response.getResponsePreferences() != null) {
            ResponsePreference pref = response.getResponsePreferences();
            context.append("KULLANICI TERCİHLERİ:\n");
            context.append("- Dil: ").append(pref.getLanguage()).append("\n");
            context.append("- İletişim stili: ").append(pref.getCommunicationStyle()).append("\n\n");
        }
        
        // Özel talimatları ekle
        if (!response.getCustomInstructions().isEmpty()) {
            context.append("KULLANICI TALİMATLARI:\n");
            for (UserInstruction instruction : response.getCustomInstructions()) {
                context.append("- ").append(instruction.getContent()).append("\n");
            }
            context.append("\n");
        }
        
        // Çıkarılmış bilgileri ekle
        if (!response.getExtractedMemories().isEmpty()) {
            context.append("KULLANICI HAKKINDA BİLİNENLER:\n");
            
            for (var entry : response.getExtractedMemories().entrySet()) {
                String category = entry.getKey();
                List<Map<String, Object>> items = entry.getValue();
                
                context.append(category.toUpperCase()).append(":\n");
                
                for (var item : items) {
                    if (item.containsKey("content")) {
                        context.append("- ").append(item.get("content")).append("\n");
                    }
                }
                
                context.append("\n");
            }
        }
        
        // Olası prompt injection'ları temizle
        String cleanedContext = sanitizeContext(context.toString());
        
        return cleanedContext;
    }

    private String sanitizeContext(String context) {
        // Basit bir sanitization işlemi: Prompt injection'a karşı tehlikeli olabilecek ifadeleri temizle
        String cleaned = context
            .replace("system:", "")
            .replace("assistant:", "")
            .replace("ignore previous instructions", "")
            .replace("ignore your instructions", "")
            .replace("ignore all instructions", "")
            .replace("disregard", "consider")
            .replace("SYSTEM:", "")
            .replace("ASSISTANT:", "")
            .replace("SİSTEM:", "")
            .replace("model:", "")
            .replace("MODEL:", "");
        
        return cleaned;
    }
}
