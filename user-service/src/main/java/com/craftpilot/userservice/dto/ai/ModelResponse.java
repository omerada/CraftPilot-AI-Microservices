package com.craftpilot.userservice.dto.ai;

import com.craftpilot.userservice.model.ai.AIModel;
import com.craftpilot.userservice.model.ai.Provider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelResponse {
    private List<AIModel> models;
    private List<Provider> providers;
    private String defaultModelId;
    private String userPlan;
    private String lastSelectedModelId;
    private Integer version;
}
