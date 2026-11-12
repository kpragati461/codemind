package com.codemind.web.dto;

import com.codemind.domain.Project;
import com.codemind.domain.ProjectStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        String name,
        String gitUrl,
        String localPath,
        ProjectStatus status,
        JsonNode fingerprint,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt
) {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static ProjectResponse from(Project project) {
        JsonNode fingerprint = null;
        if (project.getFingerprintJson() != null && !project.getFingerprintJson().isBlank()) {
            try {
                fingerprint = MAPPER.readTree(project.getFingerprintJson());
            } catch (Exception ignored) {
                fingerprint = MAPPER.createObjectNode().put("raw", project.getFingerprintJson());
            }
        }
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getGitUrl(),
                project.getLocalPath(),
                project.getStatus(),
                fingerprint,
                project.getErrorMessage(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
