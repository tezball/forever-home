package com.example.foreverhome.moderation.service.admin;

import com.example.foreverhome.moderation.domain.admin.BlockedIp;
import com.example.foreverhome.moderation.repository.admin.BlockedIpRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AdminRateLimitService {

    private final BlockedIpRepository blockedIpRepository;
    private final JdbcClient jdbcClient;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public AdminRateLimitService(BlockedIpRepository blockedIpRepository,
                                  JdbcClient jdbcClient,
                                  @Value("${main-app.url:http://localhost:8080}") String mainAppUrl,
                                  ObjectMapper objectMapper) {
        this.blockedIpRepository = blockedIpRepository;
        this.jdbcClient = jdbcClient;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(mainAppUrl)
                .build();
    }

    public RateLimitDashboard getDashboard() {
        List<RateLimitedIp> rateLimitedIps = getRateLimitedIps();
        long blockedIpsCount = blockedIpRepository.countActive();
        RateLimitConfig config = getRateLimitConfig();

        return new RateLimitDashboard(rateLimitedIps, blockedIpsCount, config);
    }

    private List<RateLimitedIp> getRateLimitedIps() {
        // Try to fetch from main app's internal API
        try {
            String response = restClient.get()
                    .uri("/api/internal/rate-limits")
                    .retrieve()
                    .body(String.class);

            JsonNode node = objectMapper.readTree(response);
            List<RateLimitedIp> ips = new ArrayList<>();
            if (node.isArray()) {
                for (JsonNode item : node) {
                    ips.add(new RateLimitedIp(
                            item.path("ip").asText(),
                            item.path("remainingTokens").asInt(),
                            item.path("maxTokens").asInt(),
                            Instant.parse(item.path("resetAt").asText())
                    ));
                }
            }
            return ips;
        } catch (Exception e) {
            // API not available yet, return empty list
            return new ArrayList<>();
        }
    }

    private RateLimitConfig getRateLimitConfig() {
        // Default rate limit configuration
        return new RateLimitConfig(100, 60, true);
    }

    public List<WhitelistedUser> getWhitelistedUsers() {
        return jdbcClient.sql("""
            SELECT id, email, name
            FROM app_users
            WHERE rate_limit_exempt = true
            ORDER BY email
            """)
                .query((rs, rowNum) -> new WhitelistedUser(
                        rs.getString("id"),
                        rs.getString("email"),
                        rs.getString("name")
                ))
                .list();
    }

    public void addToWhitelist(String userId) {
        jdbcClient.sql("UPDATE app_users SET rate_limit_exempt = true WHERE id = :userId::uuid")
                .param("userId", userId)
                .update();
    }

    public void removeFromWhitelist(String userId) {
        jdbcClient.sql("UPDATE app_users SET rate_limit_exempt = false WHERE id = :userId::uuid")
                .param("userId", userId)
                .update();
    }

    public record RateLimitDashboard(
            List<RateLimitedIp> rateLimitedIps,
            long blockedIpsCount,
            RateLimitConfig config
    ) {}

    public record RateLimitedIp(
            String ipAddress,
            int remainingTokens,
            int maxTokens,
            Instant resetAt
    ) {
        public int usagePercent() {
            if (maxTokens == 0) return 0;
            return 100 - ((remainingTokens * 100) / maxTokens);
        }
    }

    public record RateLimitConfig(
            int requestsPerMinute,
            int burstSize,
            boolean enabled
    ) {}

    public record WhitelistedUser(
            String id,
            String email,
            String name
    ) {}
}
