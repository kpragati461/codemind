package com.codemind.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class CloneRequest {

    @NotBlank(message = "repoUrl must not be blank")
    private String repoUrl;

    public String getRepoUrl() {
        return repoUrl;
    }

    public void setRepoUrl(String repoUrl) {
        this.repoUrl = repoUrl;
    }
}
