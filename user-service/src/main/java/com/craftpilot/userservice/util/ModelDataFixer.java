package com.craftpilot.userservice.util;

import com.craftpilot.userservice.model.AIModel;
import com.craftpilot.userservice.model.AIModel.AIModelBuilder;
import org.springframework.stereotype.Component;

@Component
public class ModelDataFixer {

    public AIModel fixAIModelData(AIModel model) {
        if (model == null) {
            return null;
        }

        AIModelBuilder builder = model.toBuilder();

        // AIModel verilerindeki olası hataları düzelt
        // Örneğin, isActive alanını kontrol et ve gerekirse düzelt
        if (builder.isActive() == null) {
            builder.active(true); // Varsayılan değeri true olarak ayarla
        }

        // Diğer düzeltmeler...
        // builder.setSomeField(defaultValue);

        return builder.build();
    }
}