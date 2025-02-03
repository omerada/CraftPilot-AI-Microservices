package com.craftpilot.apigateway.security;

import com.google.firebase.auth.FirebaseToken;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Getter
public class FirebaseUserDetails implements UserDetails {
    private final String uid;
    private final String email;
    private final String name;
    private final boolean emailVerified;

    public FirebaseUserDetails(FirebaseToken token) {
        this.uid = token.getUid();
        this.email = token.getEmail();
        this.name = token.getName();
        this.emailVerified = token.isEmailVerified();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
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
} 