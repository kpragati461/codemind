package com.codemind.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

/** HTTP client for the Python analysis service (see {@code analysis/}). */
@Service
public class AnalysisClient {

    private final RestClient restClient;
    private final String analysisUrl;

    public AnalysisClient(@Value("${codemind.analysis-url}") String analysisUrl) {
        this.analysisUrl = analysisUrl;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofMinutes(2));
        this.restClient = RestClient.builder().baseUrl(analysisUrl).requestFactory(factory).build();
    }

    public String fingerprint(String projectId, String localPath) throws IOException {
        try {
            return restClient.post()
                    .uri("/v1/fingerprint")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("projectId", projectId, "path", localPath))
                    .retrieve()
                    .body(String.class);
        } catch (ResourceAccessException e) {
            throw new IOException("Analysis service is not reachable at " + analysisUrl
                    + " (start it from the analysis/ folder)", e);
        }
    }
}
