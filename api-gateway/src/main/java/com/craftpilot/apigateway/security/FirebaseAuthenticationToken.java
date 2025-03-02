package com.craftpilot.apigateway.security;

import com.google.firebase.auth.FirebaseToken;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class FirebaseAuthenticationToken extends AbstractAuthenticationToken {
    private final FirebaseUserDetails userDetails;
    private final FirebaseToken firebaseToken;

    public FirebaseAuthenticationToken(FirebaseUserDetails userDetails, FirebaseToken firebaseToken) {
        super(userDetails.getAuthorities());
        this.userDetails = userDetails;
        this.firebaseToken = firebaseToken;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return firebaseToken;
    }

    @Override
    public Object getPrincipal() {
        return userDetails;
    }
}
