package com.codemind.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectServiceTest {

    @ParameterizedTest
    @CsvSource({
            "https://github.com/spring-projects/spring-petclinic.git, spring-petclinic",
            "https://github.com/spring-projects/spring-petclinic, spring-petclinic",
            "https://github.com/spring-projects/spring-petclinic/, spring-petclinic",
            "https://github.com/acme/my.github-tools.git, my.github-tools",
            "https://github.com/acme/legit.gitops, legit.gitops",
            "https://github.com/acme/repo.git?ref=main, repo",
            "git@github.com:acme/backend.git, backend",
            "git@github.com:backend.git, backend",
    })
    void resolvesNameFromUrl(String url, String expected) {
        assertThat(ProjectService.resolveName(null, url)).isEqualTo(expected);
    }

    @Test
    void explicitNameWins() {
        assertThat(ProjectService.resolveName("  My Repo ", "https://github.com/a/b.git")).isEqualTo("My Repo");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://github.com/a/b.git",
            "http://gitlab.local/a/b",
            "git@github.com:a/b.git",
    })
    void acceptsSupportedUrls(String url) {
        assertThatCode(() -> ProjectService.validateGitUrl(url)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "file:///etc/passwd",
            "ext::sh -c touch% /tmp/pwned",
            "--upload-pack=evil",
            "https:///nohost",
            "git@:a/b",
            "https://github.com/a b",
    })
    void rejectsUnsupportedUrls(String url) {
        assertThatThrownBy(() -> ProjectService.validateGitUrl(url)).isInstanceOf(IllegalArgumentException.class);
    }
}
