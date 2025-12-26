package com.example.foreverhome.moderation.service.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdminApiHealthService {

    private final RestClient mainAppClient;
    private final JdbcClient jdbcClient;
    private final S3Client s3Client;
    private final ObjectMapper objectMapper;
    private final String mainAppUrl;
    private final String bucketName;
    private final String ollamaUrl;

    public AdminApiHealthService(@Value("${main-app.url:http://localhost:8080}") String mainAppUrl,
                                  @Value("${aws.s3.bucket-name:forever-home-images}") String bucketName,
                                  @Value("${ollama.url:http://localhost:11434}") String ollamaUrl,
                                  JdbcClient jdbcClient,
                                  S3Client s3Client,
                                  ObjectMapper objectMapper) {
        this.mainAppUrl = mainAppUrl;
        this.bucketName = bucketName;
        this.ollamaUrl = ollamaUrl;
        this.jdbcClient = jdbcClient;
        this.s3Client = s3Client;
        this.objectMapper = objectMapper;
        this.mainAppClient = RestClient.builder()
                .baseUrl(mainAppUrl)
                .build();
    }

    public ApiHealthDashboard getDashboard() {
        List<HealthCheck> checks = new ArrayList<>();

        checks.add(checkMainApp());
        checks.add(checkDatabase());
        checks.add(checkS3());
        checks.add(checkOllama());

        int healthyCount = (int) checks.stream().filter(c -> "UP".equals(c.status())).count();
        int unhealthyCount = checks.size() - healthyCount;
        String overallStatus = unhealthyCount == 0 ? "UP" : (healthyCount == 0 ? "DOWN" : "DEGRADED");

        return new ApiHealthDashboard(overallStatus, checks, healthyCount, unhealthyCount, Instant.now());
    }

    public HealthCheck checkMainApp() {
        Instant start = Instant.now();
        try {
            String response = mainAppClient.get()
                    .uri("/actuator/health")
                    .retrieve()
                    .body(String.class);

            long responseTime = Duration.between(start, Instant.now()).toMillis();

            JsonNode node = objectMapper.readTree(response);
            String status = node.path("status").asText("UNKNOWN");

            return new HealthCheck(
                    "Main Application",
                    "main-app",
                    status,
                    responseTime,
                    null
            );
        } catch (Exception e) {
            long responseTime = Duration.between(start, Instant.now()).toMillis();
            return new HealthCheck(
                    "Main Application",
                    "main-app",
                    "DOWN",
                    responseTime,
                    e.getMessage()
            );
        }
    }

    public HealthCheck checkDatabase() {
        Instant start = Instant.now();
        try {
            jdbcClient.sql("SELECT 1")
                    .query(Integer.class)
                    .single();

            long responseTime = Duration.between(start, Instant.now()).toMillis();

            return new HealthCheck(
                    "PostgreSQL Database",
                    "database",
                    "UP",
                    responseTime,
                    null
            );
        } catch (Exception e) {
            long responseTime = Duration.between(start, Instant.now()).toMillis();
            return new HealthCheck(
                    "PostgreSQL Database",
                    "database",
                    "DOWN",
                    responseTime,
                    e.getMessage()
            );
        }
    }

    public HealthCheck checkS3() {
        Instant start = Instant.now();
        try {
            s3Client.headBucket(HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build());

            long responseTime = Duration.between(start, Instant.now()).toMillis();

            return new HealthCheck(
                    "AWS S3 (" + bucketName + ")",
                    "s3",
                    "UP",
                    responseTime,
                    null
            );
        } catch (Exception e) {
            long responseTime = Duration.between(start, Instant.now()).toMillis();
            return new HealthCheck(
                    "AWS S3 (" + bucketName + ")",
                    "s3",
                    "DOWN",
                    responseTime,
                    e.getMessage()
            );
        }
    }

    public HealthCheck checkOllama() {
        Instant start = Instant.now();
        try {
            RestClient ollamaClient = RestClient.builder()
                    .baseUrl(ollamaUrl)
                    .build();

            ollamaClient.get()
                    .uri("/api/tags")
                    .retrieve()
                    .body(String.class);

            long responseTime = Duration.between(start, Instant.now()).toMillis();

            return new HealthCheck(
                    "Ollama AI",
                    "ollama",
                    "UP",
                    responseTime,
                    null
            );
        } catch (Exception e) {
            long responseTime = Duration.between(start, Instant.now()).toMillis();
            return new HealthCheck(
                    "Ollama AI",
                    "ollama",
                    "DOWN",
                    responseTime,
                    e.getMessage()
            );
        }
    }

    public record ApiHealthDashboard(
            String overallStatus,
            List<HealthCheck> checks,
            int healthyCount,
            int unhealthyCount,
            Instant checkedAt
    ) {
        public double uptimePercent() {
            if (checks.isEmpty()) return 100.0;
            return (double) healthyCount / checks.size() * 100;
        }
    }

    public record HealthCheck(
            String name,
            String key,
            String status,
            long responseTimeMs,
            String error
    ) {
        public boolean isHealthy() {
            return "UP".equals(status);
        }
    }
}
