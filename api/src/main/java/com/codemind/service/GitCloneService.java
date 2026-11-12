package com.codemind.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Service
public class GitCloneService {

    private final Path workspace;

    public GitCloneService(@Value("${codemind.workspace}") String workspace) {
        this.workspace = Path.of(workspace).resolve("repos");
    }

    public Path cloneRepository(String gitUrl, String projectId) throws IOException, InterruptedException {
        Files.createDirectories(workspace);
        Path target = workspace.resolve(projectId);
        if (Files.exists(target)) {
            deleteRecursively(target);
        }

        // Output goes to a temp file rather than being read from the pipe: reading the pipe to EOF
        // blocks until git exits, which would make the timeout below unreachable.
        Path log = Files.createTempFile("codemind-clone-", ".log");
        try {
            ProcessBuilder builder = new ProcessBuilder(
                    "git", "clone", "--depth", "1", "--", gitUrl, target.toString()
            );
            builder.redirectErrorStream(true);
            builder.redirectOutput(log.toFile());
            builder.environment().put("GIT_TERMINAL_PROMPT", "0");
            Process process = builder.start();
            boolean finished = process.waitFor(5, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                throw new IOException("git clone timed out after 5 minutes");
            }
            if (process.exitValue() != 0) {
                throw new IOException("git clone failed: " + Files.readString(log).trim());
            }
            return target;
        } finally {
            Files.deleteIfExists(log);
        }
    }

    private static void deleteRecursively(Path root) throws IOException {
        try (var paths = Files.walk(root)) {
            paths.sorted((a, b) -> b.compareTo(a)).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
