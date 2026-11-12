package com.codemind.web;

import com.codemind.repo.ProjectRepository;
import com.codemind.repo.UserRepository;
import com.codemind.service.IngestionWorker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static com.codemind.web.AuthTestSupport.PASSWORD;
import static com.codemind.web.AuthTestSupport.bearer;
import static com.codemind.web.AuthTestSupport.credentials;
import static com.codemind.web.AuthTestSupport.register;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:codemind-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE")
class AuthControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    UserRepository users;

    @Autowired
    ProjectRepository projects;

    @MockitoBean
    IngestionWorker ingestionWorker;

    @BeforeEach
    void clean() {
        projects.deleteAll();
        users.deleteAll();
    }

    @Test
    void registerReturnsTokenAndStoresHashedPassword() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("Ada@Example.com", PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value(not(emptyOrNullString())))
                .andExpect(jsonPath("$.expiresAt").exists())
                .andExpect(jsonPath("$.user.email").value("ada@example.com"));

        assertThat(users.findByEmail("ada@example.com")).hasValueSatisfying(u -> {
            assertThat(u.getPasswordHash()).isNotEqualTo(PASSWORD).startsWith("$2");
        });
    }

    @Test
    void registerRejectsDuplicateEmail() throws Exception {
        register(mvc, "ada@example.com");

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("ADA@example.com", PASSWORD)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("An account with this email already exists"));
    }

    @Test
    void registerValidatesInput() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("not-an-email", PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("ada@example.com", "short")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void loginReturnsTokenThatAuthenticatesMe() throws Exception {
        register(mvc, "ada@example.com");

        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("Ada@Example.com", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("ada@example.com"))
                .andReturn().getResponse().getContentAsString();
        String token = com.jayway.jsonpath.JsonPath.read(body, "$.token");

        mvc.perform(get("/api/auth/me").with(bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ada@example.com"));
    }

    @Test
    void badPasswordIs401() throws Exception {
        register(mvc, "ada@example.com");

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("ada@example.com", "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid email or password"));
    }

    @Test
    void unknownEmailIs401WithSameMessage() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("nobody@example.com", PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid email or password"));
    }

    @Test
    void noTokenIs401WithErrorBody() throws Exception {
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Authentication required"));
        mvc.perform(get("/api/projects"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Authentication required"));
    }

    @Test
    void invalidTokenIs401() throws Exception {
        mvc.perform(get("/api/projects").with(bearer("not.a.jwt")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid or expired token. Please sign in again."));
    }

    @Test
    void tokenForDeletedUserIs401() throws Exception {
        String token = register(mvc, "ada@example.com");
        users.deleteAll();

        mvc.perform(get("/api/auth/me").with(bearer(token)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void healthStaysPublic() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk());
    }

    @Test
    void corsPreflightAllowsAuthorizationHeaderFromWebApp() throws Exception {
        mvc.perform(options("/api/projects")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, org.hamcrest.Matchers.containsStringIgnoringCase("authorization")));
    }

    @Test
    void corsRejectsOtherOrigins() throws Exception {
        mvc.perform(options("/api/projects")
                        .header(HttpHeaders.ORIGIN, "http://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }
}
