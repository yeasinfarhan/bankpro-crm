package com.bankpro.security;

import com.bankpro.exception.AuthenticationFailedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/** Reads the authenticated user of the current request from Spring Security's context. */
@Component
public class CurrentUserProvider {

    public AuthenticatedUser get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return AuthenticatedUser.fromJwt(jwt);
        }
        throw new AuthenticationFailedException("Authentication required.");
    }
}
