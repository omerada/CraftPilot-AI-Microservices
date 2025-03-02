package com.craftpilot.apigateway.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;

public class FirebaseAuthenticationToken extends AbstractAuthenticationToken {
    private final FirebaseUserDetails principal;
    private final String credentials;

    public FirebaseAuthenticationToken(FirebaseUserDetails principal, String credentials) {
        super(principal.getAuthorities());
        this.principal = principal;
        this.credentials = credentials;
        setAuthenticated(true);
    }

    @Override
    public String getCredentials() {
        return credentials;
    }

    @Override
    public FirebaseUserDetails getPrincipal() {
        return principal;
    }
}
