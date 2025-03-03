package com.craftpilot.apigateway.security;

import com.google.firebase.auth.FirebaseToken;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;

public class FirebaseAuthenticationToken extends AbstractAuthenticationToken {
    private final UserDetails principal;
    private final FirebaseToken credentials;

    public FirebaseAuthenticationToken(UserDetails principal, FirebaseToken credentials) {
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
