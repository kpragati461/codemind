package com.codemind.web.dto;

import com.codemind.service.auth.AuthService;

import java.time.Instant;

/** Returned by register and login. Send {@code token} as {@code Authorization: Bearer <token>}. */
public record AuthResponse(String token, Instant expiresAt, UserResponse user) {

    public static AuthResponse from(AuthService.Session session) {
        return new AuthResponse(
                session.token().value(),
                session.token().expiresAt(),
                UserResponse.from(session.user())
        );
    }
}
