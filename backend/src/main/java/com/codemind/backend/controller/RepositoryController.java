package com.codemind.backend.controller;

import com.codemind.backend.dto.CloneRequest;
import com.codemind.backend.service.GitCloneService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/repos")
public class RepositoryController {

    private final GitCloneService gitCloneService;

    public RepositoryController(GitCloneService gitCloneService) {
        this.gitCloneService = gitCloneService;
    }

    @PostMapping("/clone")
    public ResponseEntity<?> cloneRepo(@Valid @RequestBody CloneRequest request) {
        try {
            String localPath = gitCloneService.cloneRepository(request.getRepoUrl());
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "repoUrl", request.getRepoUrl(),
                    "localPath", localPath
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }
}
