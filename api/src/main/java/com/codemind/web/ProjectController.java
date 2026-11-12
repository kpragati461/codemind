package com.codemind.web;

import com.codemind.web.dto.CreateProjectRequest;
import com.codemind.web.dto.ProjectResponse;
import com.codemind.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "service", "codemind-api");
    }

    @GetMapping("/projects")
    public List<ProjectResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return projectService.list(CurrentUser.id(jwt)).stream().map(ProjectResponse::from).toList();
    }

    @PostMapping("/projects")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ProjectResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateProjectRequest request) {
        return ProjectResponse.from(projectService.create(request, CurrentUser.id(jwt)));
    }

    @GetMapping("/projects/{id}")
    public ProjectResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return ProjectResponse.from(projectService.get(id, CurrentUser.id(jwt)));
    }

    @PostMapping("/projects/{id}/ingest")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ProjectResponse reingest(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return ProjectResponse.from(projectService.reingest(id, CurrentUser.id(jwt)));
    }
}
