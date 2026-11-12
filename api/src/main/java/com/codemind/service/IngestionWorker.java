package com.codemind.service;

import com.codemind.domain.Project;
import com.codemind.domain.ProjectStatus;
import com.codemind.repo.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.UUID;

/**
 * Runs repository ingestion off the request thread.
 *
 * <p>This lives in its own bean on purpose: {@code @Async} only takes effect when the method is
 * called through the Spring proxy, so a call from inside {@link ProjectService} itself would run
 * synchronously and block the HTTP request for the whole clone.
 */
@Service
public class IngestionWorker {

    private static final Logger log = LoggerFactory.getLogger(IngestionWorker.class);
    private static final int MAX_ERROR_LENGTH = 1800;

    private final ProjectRepository projects;
    private final GitCloneService gitCloneService;
    private final AnalysisClient analysisClient;

    public IngestionWorker(
            ProjectRepository projects,
            GitCloneService gitCloneService,
            AnalysisClient analysisClient
    ) {
        this.projects = projects;
        this.gitCloneService = gitCloneService;
        this.analysisClient = analysisClient;
    }

    @Async
    public void ingest(UUID id) {
        Project project = projects.findById(id).orElse(null);
        if (project == null) {
            log.warn("Ingestion requested for unknown project {}", id);
            return;
        }
        project.setStatus(ProjectStatus.INDEXING);
        project.setErrorMessage(null);
        projects.save(project);

        try {
            Path local = gitCloneService.cloneRepository(project.getGitUrl(), id.toString());
            project.setLocalPath(local.toAbsolutePath().toString());
            String fingerprint = analysisClient.fingerprint(id.toString(), local.toAbsolutePath().toString());
            project.setFingerprintJson(fingerprint);
            project.setStatus(ProjectStatus.COMPLETED);
            projects.save(project);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail(project, "Ingestion was interrupted");
        } catch (Exception e) {
            log.warn("Ingestion failed for project {}", id, e);
            fail(project, e.getMessage());
        }
    }

    private void fail(Project project, String message) {
        project.setStatus(ProjectStatus.FAILED);
        project.setErrorMessage(truncate(message));
        projects.save(project);
    }

    private static String truncate(String message) {
        if (message == null || message.isBlank()) {
            return "Unknown error";
        }
        return message.length() > MAX_ERROR_LENGTH ? message.substring(0, MAX_ERROR_LENGTH) : message;
    }
}
