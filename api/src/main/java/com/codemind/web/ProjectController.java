package com.codemind.web;

import com.codemind.web.dto.CreateProjectRequest;
import com.codemind.web.dto.ProjectResponse;
import com.codemind.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
    public List<ProjectResponse> list() {
        return projectService.list().stream().map(ProjectResponse::from).toList();
    }

    @PostMapping("/projects")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ProjectResponse create(@Valid @RequestBody CreateProjectRequest request) {
        return ProjectResponse.from(projectService.create(request));
    }

    @GetMapping("/projects/{id}")
    public ProjectResponse get(@PathVariable UUID id) {
        return ProjectResponse.from(projectService.get(id));
    }

    @PostMapping("/projects/{id}/ingest")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ProjectResponse reingest(@PathVariable UUID id) {
        return ProjectResponse.from(projectService.reingest(id));
    }
}
