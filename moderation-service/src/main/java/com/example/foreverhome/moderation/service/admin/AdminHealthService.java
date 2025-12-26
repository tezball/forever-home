package com.example.foreverhome.moderation.service.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminHealthService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String mainAppUrl;

    public AdminHealthService(@Value("${main-app.url:http://localhost:8080}") String mainAppUrl,
                               ObjectMapper objectMapper) {
        this.mainAppUrl = mainAppUrl;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(mainAppUrl)
                .build();
    }

    public SystemHealth getSystemHealth() {
        Map<String, ComponentHealth> components = new HashMap<>();
        String overallStatus = "UP";

        // Check main app health
        try {
            String response = restClient.get()
                    .uri("/actuator/health")
                    .retrieve()
                    .body(String.class);

            JsonNode healthNode = objectMapper.readTree(response);
            String appStatus = healthNode.path("status").asText("UNKNOWN");
            components.put("main-app", new ComponentHealth(appStatus, Map.of()));

            if (healthNode.has("components")) {
                JsonNode componentsNode = healthNode.get("components");
                componentsNode.fieldNames().forEachRemaining(name -> {
                    JsonNode comp = componentsNode.get(name);
                    String status = comp.path("status").asText("UNKNOWN");
                    Map<String, Object> details = new HashMap<>();
                    if (comp.has("details")) {
                        comp.get("details").fieldNames().forEachRemaining(key ->
                            details.put(key, comp.get("details").get(key).asText())
                        );
                    }
                    components.put(name, new ComponentHealth(status, details));
                });
            }

            if (!"UP".equals(appStatus)) {
                overallStatus = appStatus;
            }
        } catch (Exception e) {
            components.put("main-app", new ComponentHealth("DOWN", Map.of("error", e.getMessage())));
            overallStatus = "DOWN";
        }

        return new SystemHealth(overallStatus, components, Instant.now());
    }

    public List<ErrorLogEntry> getRecentErrors(int limit) {
        // In a real implementation, this would fetch from Loki or application logs
        // For now, return empty list as placeholder
        return new ArrayList<>();
    }

    public MetricsSummary getMetricsSummary() {
        try {
            String response = restClient.get()
                    .uri("/actuator/metrics")
                    .retrieve()
                    .body(String.class);

            JsonNode metricsNode = objectMapper.readTree(response);
            List<String> availableMetrics = new ArrayList<>();
            if (metricsNode.has("names")) {
                metricsNode.get("names").forEach(node -> availableMetrics.add(node.asText()));
            }

            // Get specific metrics
            double jvmMemoryUsed = getMetricValue("jvm.memory.used");
            double jvmMemoryMax = getMetricValue("jvm.memory.max");
            double cpuUsage = getMetricValue("system.cpu.usage");
            long httpRequestsTotal = (long) getMetricValue("http.server.requests");

            return new MetricsSummary(
                    jvmMemoryUsed,
                    jvmMemoryMax,
                    cpuUsage,
                    httpRequestsTotal,
                    availableMetrics.size()
            );
        } catch (Exception e) {
            return new MetricsSummary(0, 0, 0, 0, 0);
        }
    }

    private double getMetricValue(String metricName) {
        try {
            String response = restClient.get()
                    .uri("/actuator/metrics/{name}", metricName)
                    .retrieve()
                    .body(String.class);

            JsonNode node = objectMapper.readTree(response);
            if (node.has("measurements") && node.get("measurements").isArray()) {
                return node.get("measurements").get(0).path("value").asDouble(0);
            }
        } catch (Exception e) {
            // Metric not available
        }
        return 0;
    }

    public record SystemHealth(
            String status,
            Map<String, ComponentHealth> components,
            Instant checkedAt
    ) {}

    public record ComponentHealth(
            String status,
            Map<String, Object> details
    ) {}

    public record ErrorLogEntry(
            Instant timestamp,
            String level,
            String message,
            String source
    ) {}

    public record MetricsSummary(
            double jvmMemoryUsed,
            double jvmMemoryMax,
            double cpuUsage,
            long httpRequestsTotal,
            int availableMetrics
    ) {
        public double memoryUsagePercent() {
            if (jvmMemoryMax == 0) return 0;
            return (jvmMemoryUsed / jvmMemoryMax) * 100;
        }
    }
}
