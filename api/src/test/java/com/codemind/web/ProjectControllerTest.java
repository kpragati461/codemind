package com.codemind.web;

import com.codemind.domain.ProjectStatus;
import com.codemind.repo.ProjectRepository;
import com.codemind.repo.UserRepository;
import com.codemind.service.IngestionWorker;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static com.codemind.web.AuthTestSupport.bearer;
import static com.codemind.web.AuthTestSupport.register;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:codemind-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE")
class ProjectControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ProjectRepository projects;

    @Autowired
    UserRepository users;

    @MockitoBean
    IngestionWorker ingestionWorker;

    String token;

    @BeforeEach
    void setUp() throws Exception {
        projects.deleteAll();
        users.deleteAll();
        token = register(mvc, "owner@example.com");
    }

    @Test
    void createQueuesProjectAndHandsOffToWorker() throws Exception {
        mvc.perform(post("/api/projects").with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gitUrl\":\"https://github.com/acme/my.github-tools.git\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andExpect(jsonPath("$.name").value("my.github-tools"));

        assertThat(projects.findAll()).singleElement()
                .satisfies(p -> assertThat(p.getStatus()).isEqualTo(ProjectStatus.QUEUED));
        verify(ingestionWorker).ingest(any(UUID.class));
    }

    @Test
    void createRequiresAuth() throws Exception {
        mvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gitUrl\":\"https://github.com/acme/repo.git\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Authentication required"));
        assertThat(projects.count()).isZero();
    }

    @Test
    void rejectsBadUrl() throws Exception {
        mvc.perform(post("/api/projects").with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gitUrl\":\"file:///etc/passwd\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listReturnsNewestFirst() throws Exception {
        for (String repo : new String[]{"first", "second", "third"}) {
            create(token, repo);
            Thread.sleep(5); // keep createdAt strictly increasing on coarse clocks
        }

        mvc.perform(get("/api/projects").with(bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name").value(contains("third", "second", "first")));
    }

    @Test
    void reingestUnknownProjectIs404() throws Exception {
        mvc.perform(post("/api/projects/" + UUID.randomUUID() + "/ingest").with(bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void usersCannotSeeEachOthersProjects() throws Exception {
        String ownersId = create(token, "private-repo");
        String otherToken = register(mvc, "intruder@example.com");

        mvc.perform(get("/api/projects").with(bearer(otherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/projects/" + ownersId).with(bearer(otherToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Project not found: " + ownersId));
        mvc.perform(post("/api/projects/" + ownersId + "/ingest").with(bearer(otherToken)))
                .andExpect(status().isNotFound());

        // The owner still sees it.
        mvc.perform(get("/api/projects/" + ownersId).with(bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("private-repo"));
        mvc.perform(get("/api/projects").with(bearer(token)))
                .andExpect(jsonPath("$[*].name").value(contains("private-repo")));
    }

    private String create(String bearerToken, String repo) throws Exception {
        String body = mvc.perform(post("/api/projects").with(bearer(bearerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gitUrl\":\"https://github.com/acme/" + repo + ".git\"}"))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }
}
