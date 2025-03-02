package com.craftpilot.apigateway.security;

import com.google.firebase.auth.FirebaseToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class FirebaseUserDetails implements UserDetails {
    private final String uid;
    private final String email;
    private final String name;
    private final boolean emailVerified;
    private final String role;

    public FirebaseUserDetails(FirebaseToken token) {
        this.uid = token.getUid();
        this.email = token.getEmail();
        this.name = token.getName();
        this.emailVerified = token.isEmailVerified();
        this.role = extractRole(token);
    }

    private String extractRole(FirebaseToken token) {
        Map<String, Object> claims = token.getClaims();
        if (claims.containsKey("role")) {
            return claims.get("role").toString();
        } else if (claims.containsKey("admin") && Boolean.TRUE.equals(claims.get("admin"))) {
            return "ADMIN";
        }
        return "USER";
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return emailVerified;
    }
    
    public String getRole() {
        return this.role;
    }
    
    public String getUid() {
        return this.uid;
    }
    
    public String getEmail() {
        return this.email;
    }
    
    public String getName() {
        return this.name;
    }
    
    public boolean isEmailVerified() {
        return this.emailVerified;
    }
}