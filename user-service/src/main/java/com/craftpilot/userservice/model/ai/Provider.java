package com.craftpilot.userservice.model.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import com.google.cloud.firestore.annotation.DocumentId;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TypeAlias("provider")
public class Provider {
    @Id
    @DocumentId
    private String name;
    private String icon;
    private String description;
    private List<AIModel> models;
}
