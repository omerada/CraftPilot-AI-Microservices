package com.craftpilot.userservice.util;

import com.craftpilot.userservice.model.ai.AIModel;
import org.springframework.stereotype.Component;

@Component
public class ModelDataFixer {

    public AIModel fixAIModelData(AIModel model) {
        if (model == null) {
            return null;
        }

        AIModel.AIModelBuilder builder = model.toBuilder();
 
        if (model.isActive() == false) {
            builder.active(true); 
        } 

        return builder.build();
    }
}