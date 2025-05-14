package com.craftpilot.userservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {
    @Id
    private String id;

    @Indexed(unique = true)
    private String externalId; // Firebase UID yerine daha genel bir tanımlayıcı

    private String name;

    @Indexed
    private String email;

    private String photoUrl;
    private String phoneNumber;
    private Map<String, Boolean> roles;
    private List<String> favoriteModelIds;
    private boolean emailVerified;
    private boolean enabled;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    // UserService sınıfında kullanılacak ek alanlar ve metotlar
    private LocalDateTime lastLoginAt;
    
    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
    
    public String getDisplayName() {
        return this.name;
    }
    
    public void setDisplayName(String name) {
        this.name = name;
    }
    
    public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) {
        this.updatedAt = lastUpdatedAt;
    }

    // Veri dönüştürme yardımcı yöntemi
    public static User fromMap(Map<String, Object> data, String externalId) {
        User user = new User();
        user.setExternalId(externalId);

        if (data.containsKey("name")) {
            user.setName((String) data.get("name"));
        }

        if (data.containsKey("email")) {
            user.setEmail((String) data.get("email"));
        }

        if (data.containsKey("photoUrl")) {
            user.setPhotoUrl((String) data.get("photoUrl"));
        }

        if (data.containsKey("phoneNumber")) {
            user.setPhoneNumber((String) data.get("phoneNumber"));
        }

        if (data.containsKey("roles")) {
            @SuppressWarnings("unchecked")
            Map<String, Boolean> roles = (Map<String, Boolean>) data.get("roles");
            user.setRoles(roles);
        }

        if (data.containsKey("favoriteModelIds")) {
            @SuppressWarnings("unchecked")
            List<String> favoriteModelIds = (List<String>) data.get("favoriteModelIds");
            user.setFavoriteModelIds(favoriteModelIds);
        }

        if (data.containsKey("emailVerified")) {
            user.setEmailVerified((Boolean) data.get("emailVerified"));
        }

        if (data.containsKey("enabled")) {
            user.setEnabled((Boolean) data.get("enabled"));
        }

        return user;
    }
}
