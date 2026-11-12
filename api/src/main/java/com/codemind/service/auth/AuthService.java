package com.codemind.service.auth;

import com.codemind.domain.User;
import com.codemind.repo.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    /** Compared against when the email is unknown, so a miss costs as much as a wrong password. */
    private final String dummyHash;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.dummyHash = passwordEncoder.encode("codemind-timing-guard");
    }

    public Session register(String email, String password) {
        String normalized = normalizeEmail(email);
        if (users.existsByEmail(normalized)) {
            throw new IllegalStateException("An account with this email already exists");
        }
        User user = new User();
        user.setEmail(normalized);
        user.setPasswordHash(passwordEncoder.encode(password));
        User saved = users.save(user);
        return new Session(saved, tokenService.issue(saved));
    }

    public Session login(String email, String password) {
        User user = users.findByEmail(normalizeEmail(email)).orElse(null);
        String hash = user != null ? user.getPasswordHash() : dummyHash;
        boolean matches = passwordEncoder.matches(password, hash);
        if (user == null || !matches) {
            throw new BadCredentialsException("Invalid email or password");
        }
        return new Session(user, tokenService.issue(user));
    }

    /**
     * Loads the user a valid token was issued to. A token can outlive its user (for example after
     * the dev database is reset), so a miss is an authentication failure, not a server error.
     */
    public User requireUser(UUID id) {
        return users.findById(id)
                .orElseThrow(() -> new BadCredentialsException("Your session is no longer valid. Please sign in again."));
    }

    static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public record Session(User user, TokenService.IssuedToken token) {
    }
}
