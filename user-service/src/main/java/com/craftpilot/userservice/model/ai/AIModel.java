package com.craftpilot.userservice.model.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "ai_models")
public class AIModel {
    @Id
    private String id;

    @Indexed
    private String modelId;
    private String modelName;
    private String provider;
    private String providerId; // Provider ID için yeni alan ekliyoruz
    private Integer maxInputTokens;
    private String requiredPlan;
    private Integer creditCost; // Her kullanımda tüketilecek kredi miktarı
    private String creditType; // Kredi tipi (STANDARD veya ADVANCED)

    @Indexed
    private String category; // Model kategorisi (basic, standard, premium gibi)
    private Integer contextLength; // Modelin bağlam penceresi uzunluğu
    
    // providerId için getter ve setter metotları
    public String getProviderId() {
        return providerId;
    }
    
    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }
}
