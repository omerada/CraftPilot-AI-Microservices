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
    private String modelId;
    private String modelName;
    private String provider;
    private Integer maxInputTokens;
    private String requiredPlan;
    private Integer creditCost; // Her kullanımda tüketilecek kredi miktarı
    private String creditType;  // Kredi tipi (STANDARD veya ADVANCED)
    private String category; // Model kategorisi (basic, standard, premium gibi)
    private Integer contextLength; // Modelin bağlam penceresi uzunluğu
}
