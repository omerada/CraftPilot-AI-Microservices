package com.craftpilot.userservice.model.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "providers")
public class Provider {
    @Id
    private String name;
    private String icon;
    private String description;
    private List<AIModel> models;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * MongoDB ID'yi döndürür. Bu durumda name alanıdır.
     * @return Provider ID'si (name alanı)
     */
    public String getId() {
        return this.name;
    }
    
    /**
     * Name alanı atanırken null değeri kontrolü yapar.
     * @param name atanacak name değeri
     */
    public void setName(String name) {
        // Null değerleri için varsayılan değer kullan
        this.name = (name != null && !name.trim().isEmpty()) ? name : "Bilinmeyen";
    }
}
