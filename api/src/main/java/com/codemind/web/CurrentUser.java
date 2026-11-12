package com.codemind.web;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

/** Reads the signed-in user's id from the verified token (its {@code sub} claim). */
final class CurrentUser {

    private CurrentUser() {
    }

    static UUID id(Jwt jwt) {
        if (jwt == null) {
            throw new BadCredentialsException("Authentication required");
        }
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BadCredentialsException("Invalid or expired token. Please sign in again.");
        }
    }
}
