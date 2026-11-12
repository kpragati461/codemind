package com.codemind.config;

import com.codemind.service.auth.JwtSettings;
import com.codemind.service.auth.TokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.util.Map;

/**
 * Stateless bearer-token security.
 *
 * <p>{@code /api/health}, {@code /api/auth/register} and {@code /api/auth/login} are public;
 * everything else, including {@code /api/projects/**} and {@code /api/auth/me}, needs a valid
 * {@code Authorization: Bearer <token>} header. CORS is still configured in {@link WebConfig}:
 * {@code cors()} picks up the Spring MVC mappings, and preflight requests are let through before
 * authentication so the browser can send the {@code Authorization} header.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        AuthenticationEntryPoint unauthorized = (request, response, ex) -> {
            String message = ex instanceof OAuth2AuthenticationException
                    ? "Invalid or expired token. Please sign in again."
                    : "Authentication required";
            response.setHeader("WWW-Authenticate", "Bearer");
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, message, objectMapper);
        };
        AccessDeniedHandler forbidden = (request, response, ex) ->
                writeError(response, HttpServletResponse.SC_FORBIDDEN, "Access denied", objectMapper);

        http
                .csrf(AbstractHttpConfigurer::disable) // no cookies or sessions, so no CSRF surface
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                // The H2 console renders in frames; it has its own login and is disabled under the postgres profile.
                .headers(h -> h.frameOptions(f -> f.sameOrigin()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/health", "/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers("/h2/**", "/error").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(rs -> rs
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(unauthorized)
                        .accessDeniedHandler(forbidden))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(unauthorized)
                        .accessDeniedHandler(forbidden));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    JwtEncoder jwtEncoder(JwtSettings settings) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(settings.key()));
    }

    @Bean
    JwtDecoder jwtDecoder(JwtSettings settings) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(settings.key())
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        // Checks expiry/not-before (with the default clock skew) and that we issued the token.
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(TokenService.ISSUER));
        return decoder;
    }

    /** Same {@code {"error": "..."}} shape as {@link com.codemind.web.RestExceptionHandler}. */
    private static void writeError(HttpServletResponse response, int status, String message, ObjectMapper mapper)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), Map.of("error", message));
    }
}
