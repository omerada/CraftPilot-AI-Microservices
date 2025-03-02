package com.craftpilot.apigateway.security;

import com.google.firebase.auth.FirebaseToken;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.Collections;

public class FirebaseAuthenticationToken extends AbstractAuthenticationToken {
    private final FirebaseUserDetails principal;
    private final FirebaseToken credentials;

    public FirebaseAuthenticationToken(FirebaseUserDetails principal, FirebaseToken credentials) {
        // Methodunu çağırmak yerine principal'ın authorities'ini kullanalım
        super(principal.getAuthorities());
        this.principal = principal;
        this.credentials = credentials;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return credentials;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }
}
