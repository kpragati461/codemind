package com.codemind.service.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** The {@code codemind.jwt.*} settings: the HMAC signing key and how long a token stays valid. */
@Component
public class JwtSettings {

    private static final int MIN_SECRET_BYTES = 32; // HS256 requires a key of at least 256 bits

    private final SecretKey key;
    private final Duration ttl;

    public JwtSettings(
            @Value("${codemind.jwt.secret}") String secret,
            @Value("${codemind.jwt.ttl}") Duration ttl
    ) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("codemind.jwt.secret must be at least " + MIN_SECRET_BYTES + " bytes");
        }
        this.key = new SecretKeySpec(bytes, "HmacSHA256");
        this.ttl = ttl;
    }

    public SecretKey key() {
        return key;
    }

    public Duration ttl() {
        return ttl;
    }
}
