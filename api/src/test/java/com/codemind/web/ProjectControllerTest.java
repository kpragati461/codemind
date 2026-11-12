package com.codemind.web;

import com.codemind.domain.ProjectStatus;
import com.codemind.repo.ProjectRepository;
import com.codemind.service.IngestionWorker;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
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

    @MockitoBean
    IngestionWorker ingestionWorker;

    @BeforeEach
    void clean() {
        projects.deleteAll();
    }

    @Test
    void createQueuesProjectAndHandsOffToWorker() throws Exception {
        mvc.perform(post("/api/projects")
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
    void rejectsBadUrl() throws Exception {
        mvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gitUrl\":\"file:///etc/passwd\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reingestUnknownProjectIs404() throws Exception {
        mvc.perform(post("/api/projects/" + UUID.randomUUID() + "/ingest"))
                .andExpect(status().isNotFound());
    }
}
