package com.codemind.backend.service;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class GitCloneService {

    @Value("${codemind.workspace-dir}")
    private String workspaceDir;

    public String cloneRepository(String repoUrl) throws GitAPIException, IOException {
        String repoName = extractRepoName(repoUrl);
        Path targetPath = Path.of(workspaceDir, repoName);

        if (Files.exists(targetPath)) {
            deleteDirectory(targetPath.toFile());
        }
        Files.createDirectories(targetPath.getParent());

        Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(targetPath.toFile())
                .call()
                .close();

        return targetPath.toAbsolutePath().toString();
    }

    private String extractRepoName(String repoUrl) {
        String cleaned = repoUrl.trim();
        if (cleaned.endsWith("/")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        if (cleaned.endsWith(".git")) {
            cleaned = cleaned.substring(0, cleaned.length() - 4);
        }
        String[] parts = cleaned.split("/");
        return parts[parts.length - 1];
    }

    private void deleteDirectory(File dir) throws IOException {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteDirectory(f);
                } else {
                    Files.deleteIfExists(f.toPath());
                }
            }
        }
        Files.deleteIfExists(dir.toPath());
    }
}
