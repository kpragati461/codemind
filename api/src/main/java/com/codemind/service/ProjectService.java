package com.codemind.service;

import com.codemind.domain.Project;
import com.codemind.domain.ProjectStatus;
import com.codemind.repo.ProjectRepository;
import com.codemind.service.auth.AuthService;
import com.codemind.web.dto.CreateProjectRequest;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class ProjectService {

    private final ProjectRepository projects;
    private final IngestionWorker ingestionWorker;
    private final AuthService authService;

    public ProjectService(ProjectRepository projects, IngestionWorker ingestionWorker, AuthService authService) {
        this.projects = projects;
        this.ingestionWorker = ingestionWorker;
        this.authService = authService;
    }

    /**
     * Registers a repository and queues it for ingestion.
     *
     * <p>Deliberately not {@code @Transactional}: {@code save()} must commit before the async worker
     * looks the project up, otherwise the worker can run before the row is visible and the project
     * stays QUEUED forever.
     */
    public Project create(CreateProjectRequest request, UUID ownerId) {
        String gitUrl = request.gitUrl().trim();
        validateGitUrl(gitUrl);

        Project project = new Project();
        project.setOwner(authService.requireUser(ownerId));
        project.setGitUrl(gitUrl);
        project.setName(resolveName(request.name(), gitUrl));
        project.setStatus(ProjectStatus.QUEUED);
        Project saved = projects.save(project);
        ingestionWorker.ingest(saved.getId());
        return saved;
    }

    public Project reingest(UUID id, UUID ownerId) {
        Project project = get(id, ownerId);
        if (project.getStatus() == ProjectStatus.INDEXING) {
            throw new IllegalStateException("Project is already being indexed");
        }
        project.setStatus(ProjectStatus.QUEUED);
        project.setErrorMessage(null);
        Project saved = projects.save(project);
        ingestionWorker.ingest(id);
        return saved;
    }

    public List<Project> list(UUID ownerId) {
        return projects.findAllByOwnerIdOrderByCreatedAtDesc(ownerId);
    }

    /**
     * Another user's project is reported as not found rather than forbidden, so ids can't be probed
     * to learn which projects exist.
     */
    public Project get(UUID id, UUID ownerId) {
        return projects.findByIdAndOwnerId(id, ownerId).orElseThrow(() -> new ProjectNotFoundException(id));
    }

    /**
     * Uses the explicit name if given, otherwise the last path segment of the URL without a
     * trailing ".git". Works for both https URLs and scp-style SSH URLs (git@host:owner/repo.git).
     */
    static String resolveName(String requested, String gitUrl) {
        if (requested != null && !requested.isBlank()) {
            return requested.trim();
        }
        String path = gitUrl.trim();
        int query = indexOfAny(path, '?', '#');
        if (query >= 0) {
            path = path.substring(0, query);
        }
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        if (path.toLowerCase(Locale.ROOT).endsWith(".git")) {
            path = path.substring(0, path.length() - 4);
        }
        int cut = Math.max(path.lastIndexOf('/'), path.lastIndexOf(':'));
        String name = cut >= 0 ? path.substring(cut + 1) : path;
        return name.isBlank() ? "untitled-repo" : name;
    }

    static void validateGitUrl(String gitUrl) {
        String lower = gitUrl.toLowerCase(Locale.ROOT);
        if (lower.startsWith("https://") || lower.startsWith("http://")) {
            URI uri;
            try {
                uri = URI.create(gitUrl);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("gitUrl is not a valid URL");
            }
            if (uri.getHost() == null) {
                throw new IllegalArgumentException("gitUrl host is missing");
            }
            return;
        }
        if (lower.startsWith("git@") && gitUrl.indexOf(':') > "git@".length()) {
            return;
        }
        throw new IllegalArgumentException("gitUrl must be an http(s) URL or git@host:owner/repo");
    }

    private static int indexOfAny(String s, char a, char b) {
        int i = s.indexOf(a);
        int j = s.indexOf(b);
        if (i < 0) return j;
        if (j < 0) return i;
        return Math.min(i, j);
    }
}
