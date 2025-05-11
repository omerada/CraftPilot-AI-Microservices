package com.craftpilot.userservice.model.user.entity;

import com.craftpilot.userservice.model.user.enums.UserRole;
import com.craftpilot.userservice.model.user.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class UserEntity {
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String email;
    
    @Indexed(unique = true)
    private String username;
    
    private String displayName;
    private String photoUrl;
    
    @Indexed
    private UserRole role;
    
    @Indexed
    private UserStatus status;
    
    private Map<String, Object> preferences;
    private Map<String, Object> metadata;
    
    @Indexed
    private Long createdAt;
    
    private Long updatedAt;
}
