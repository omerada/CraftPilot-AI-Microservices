package com.craftpilot.userservice.model.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import com.google.cloud.firestore.annotation.DocumentId;

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
    private String category; // Model kategorisi (basic, standard, premium gibi)
    private String architecture; // Modelin mimarisi
    private Integer contextLength; // Modelin bağlam penceresi uzunluğu
}
