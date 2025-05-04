package com.craftpilot.llmservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractedUserInfo {
    private String userId;
    private String information;
    private String context; // Bilginin kaynağı - örn. "chat-request", "stream-request" gibi
    private Instant timestamp;
    
    // Context alanı bilgi kaynağını belirlediği için hala faydalı
    // Ancak uzun içerikler yerine standart tanımlayıcılar kullanarak optimize edilmeli
}
