package com.codemind.backend.dto;

import java.util.List;

public class RepoFingerprint {

    private String projectType;      // "maven", "gradle", or "unknown"
    private String groupId;
    private String artifactId;
    private int javaFileCount;
    private List<String> javaFiles;  // relative paths from repo root

    public RepoFingerprint(String projectType, String groupId, String artifactId,
                           int javaFileCount, List<String> javaFiles) {
        this.projectType = projectType;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.javaFileCount = javaFileCount;
        this.javaFiles = javaFiles;
    }

    public String getProjectType() { return projectType; }
    public String getGroupId() { return groupId; }
    public String getArtifactId() { return artifactId; }
    public int getJavaFileCount() { return javaFileCount; }
    public List<String> getJavaFiles() { return javaFiles; }
}
