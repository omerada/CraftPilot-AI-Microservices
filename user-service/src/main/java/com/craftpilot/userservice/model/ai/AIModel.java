package com.craftpilot.userservice.model.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import com.google.cloud.firestore.annotation.DocumentId;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TypeAlias("aiModel")
public class AIModel {
    @Id
    @DocumentId
    private String id;
    private String value;
    private String label;
    private String description;
    private String badge;
    private Boolean popular;
    private String provider;
    private String providerIcon;
    private Integer maxTokens;
    private Integer maxInputTokens;
    private String requiredPlan;
    private Integer creditCost; // Her kullanımda tüketilecek kredi miktarı
    private String creditType;  // Kredi tipi (STANDARD veya ADVANCED)
    private String category; // Model kategorisi (basic, standard, premium gibi)

    // Yeni alanlar
    private Long created; // Model oluşturma zaman damgası
    private Integer contextLength; // Modelin bağlam penceresi uzunluğu
    
    // Mimari bilgileri (String yerine nesne olarak)
    private Architecture architecture;
    
    // Fiyatlandırma bilgileri
    private Pricing pricing;
    
    // Sağlayıcı bilgileri
    private TopProvider topProvider;
    
    // İstek limitleri
    private Object perRequestLimits;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Architecture {
        private String modality;
        private List<String> inputModalities;
        private List<String> outputModalities;
        private String tokenizer;
        private String instructType;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Pricing {
        private String prompt;
        private String completion;
        private String request;
        private String image;
        private String webSearch;
        private String internalReasoning;
        private String inputCacheRead;
        private String inputCacheWrite;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopProvider {
        private Integer contextLength;
        private Integer maxCompletionTokens;
        private Boolean isModerated;
    }
}
