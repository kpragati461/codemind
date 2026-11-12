package com.codemind.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
        @NotBlank @Size(max = 1024) String gitUrl,
        @Size(max = 200) String name
) {
}
