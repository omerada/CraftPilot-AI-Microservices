package com.craftpilot.apigateway.security;

import com.google.firebase.auth.FirebaseToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FirebaseUserDetails implements UserDetails {
    private final String uid;
    private final String email;
    private final boolean emailVerified;
    private final String name;
    private final String picture;
    private final String role;
    private final Collection<GrantedAuthority> authorities;

    public FirebaseUserDetails(FirebaseToken firebaseToken) {
        this.uid = firebaseToken.getUid();
        this.email = firebaseToken.getEmail();
        this.emailVerified = firebaseToken.isEmailVerified();
        this.name = firebaseToken.getName();
        this.picture = firebaseToken.getPicture();
        this.role = determineRole(firebaseToken);
        this.authorities = buildAuthorities(this.role);
    }

    private String determineRole(FirebaseToken token) {
        // Normalde burada gerçek rol mantığı olacak
        // Örneğin token'ın claims'inden rol bilgisini çekebiliriz
        // Şimdilik basitçe USER rolü veriyoruz
        return "USER";
    }

    private Collection<GrantedAuthority> buildAuthorities(String role) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        return authorities;
    }

    public String getUid() {
        return uid;
    }

    public String getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return null; // Firebase Auth kullanıyoruz, şifreleri burada tutmuyoruz
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
        return true;
    }
}
