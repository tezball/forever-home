package com.example.foreverhome.config;

import com.example.foreverhome.service.MetricsService;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting filter for authentication and resource creation endpoints.
 *
 * Limits:
 * - Login: 10 requests per minute per IP
 * - Register: 5 requests per minute per IP
 * - Password reset: 3 requests per minute per IP
 * - Pet creation: 10 requests per day per IP (prevents S3 cost abuse)
 * - Image upload: 50 requests per day per IP (prevents S3 cost abuse)
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitFilter.class);

    // Different buckets for different endpoint types
    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> registerBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> passwordResetBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> resendVerificationBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> petCreationBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> imageUploadBuckets = new ConcurrentHashMap<>();
    private final MetricsService metricsService;
    private final JdbcClient jdbcClient;
    private final List<String> trustedProxies;
    private final boolean trustProxy;

    // Cache for blocked IPs (refreshed periodically)
    private volatile Set<String> blockedIpsCache = new HashSet<>();
    private volatile Instant blockedIpsCacheExpiry = Instant.MIN;
    private static final Duration BLOCKED_IPS_CACHE_TTL = Duration.ofMinutes(1);

    public RateLimitFilter(
            MetricsService metricsService,
            JdbcClient jdbcClient,
            @Value("${app.security.trusted-proxies:}") String trustedProxiesConfig,
            @Value("${app.security.trust-proxy:false}") boolean trustProxy) {
        this.metricsService = metricsService;
        this.jdbcClient = jdbcClient;
        this.trustProxy = trustProxy;
        this.trustedProxies = trustedProxiesConfig.isBlank()
            ? List.of()
            : Arrays.asList(trustedProxiesConfig.split(","));

        if (trustProxy) {
            logger.info("X-Forwarded-For header parsing enabled. Trusted proxies: {}",
                trustedProxies.isEmpty() ? "ALL (not recommended for production)" : trustedProxies);
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();
        String clientIp = getClientIp(request);

        // Check if IP is blocked
        if (isIpBlocked(clientIp)) {
            logger.warn("Blocked IP {} attempted to access {}", clientIp, path);
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Access denied\"}");
            return;
        }

        // Rate limit auth endpoints
        if (path.startsWith("/api/auth/")) {
            Bucket bucket = getBucketForAuthRequest(path, clientIp);

            if (bucket != null && !bucket.tryConsume(1)) {
                logger.warn("Rate limit exceeded for {} from IP {}", path, clientIp);
                metricsService.recordRateLimitHit(path, clientIp);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Too many requests. Please try again later.\",\"retryAfter\":60}");
                return;
            }
        }

        // Rate limit pet creation (POST /api/pets)
        if ("POST".equalsIgnoreCase(method) && path.equals("/api/pets")) {
            Bucket bucket = petCreationBuckets.computeIfAbsent(clientIp, this::createPetCreationBucket);
            if (!bucket.tryConsume(1)) {
                logger.warn("Pet creation rate limit exceeded from IP {}", clientIp);
                metricsService.recordRateLimitHit(path, clientIp);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Pet creation limit reached. Maximum 10 pets per day.\",\"retryAfter\":86400}");
                return;
            }
        }

        // Rate limit image uploads (POST /api/pets/{id}/images)
        if ("POST".equalsIgnoreCase(method) && path.matches("/api/pets/[^/]+/images")) {
            Bucket bucket = imageUploadBuckets.computeIfAbsent(clientIp, this::createImageUploadBucket);
            if (!bucket.tryConsume(1)) {
                logger.warn("Image upload rate limit exceeded from IP {}", clientIp);
                metricsService.recordRateLimitHit(path, clientIp);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Image upload limit reached. Maximum 50 images per day.\",\"retryAfter\":86400}");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private Bucket getBucketForAuthRequest(String path, String clientIp) {
        if (path.contains("/login")) {
            return loginBuckets.computeIfAbsent(clientIp, this::createLoginBucket);
        } else if (path.contains("/register")) {
            return registerBuckets.computeIfAbsent(clientIp, this::createRegisterBucket);
        } else if (path.contains("/forgot-password") || path.contains("/reset-password")) {
            return passwordResetBuckets.computeIfAbsent(clientIp, this::createPasswordResetBucket);
        } else if (path.contains("/resend-verification")) {
            return resendVerificationBuckets.computeIfAbsent(clientIp, this::createResendVerificationBucket);
        }
        return null; // No rate limit for other auth endpoints
    }

    private Bucket createLoginBucket(String key) {
        // 10 login attempts per minute per IP
        return Bucket.builder()
                .addLimit(Bandwidth.simple(10, Duration.ofMinutes(1)))
                .build();
    }

    private Bucket createRegisterBucket(String key) {
        // 5 registration attempts per minute per IP
        return Bucket.builder()
                .addLimit(Bandwidth.simple(5, Duration.ofMinutes(1)))
                .build();
    }

    private Bucket createPasswordResetBucket(String key) {
        // 3 password reset attempts per minute per IP
        return Bucket.builder()
                .addLimit(Bandwidth.simple(3, Duration.ofMinutes(1)))
                .build();
    }

    private Bucket createResendVerificationBucket(String key) {
        // 3 resend verification attempts per hour per IP (stricter to prevent abuse)
        return Bucket.builder()
                .addLimit(Bandwidth.simple(3, Duration.ofHours(1)))
                .build();
    }

    private Bucket createPetCreationBucket(String key) {
        // 10 pet creations per day per IP (prevents S3 cost abuse)
        return Bucket.builder()
                .addLimit(Bandwidth.simple(10, Duration.ofDays(1)))
                .build();
    }

    private Bucket createImageUploadBucket(String key) {
        // 50 image uploads per day per IP (prevents S3 cost abuse)
        return Bucket.builder()
                .addLimit(Bandwidth.simple(50, Duration.ofDays(1)))
                .build();
    }

    private String getClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();

        // Only parse X-Forwarded-For if we're configured to trust proxies
        if (!trustProxy) {
            return remoteAddr;
        }

        // If we have a trusted proxies list, verify the request came from one
        if (!trustedProxies.isEmpty() && !trustedProxies.contains(remoteAddr)) {
            logger.debug("Request from {} not in trusted proxies list, ignoring X-Forwarded-For", remoteAddr);
            return remoteAddr;
        }

        // Parse X-Forwarded-For header from trusted proxy
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // Take the first IP in the chain (original client)
            String clientIp = xForwardedFor.split(",")[0].trim();
            logger.debug("Extracted client IP {} from X-Forwarded-For (via proxy {})", clientIp, remoteAddr);
            return clientIp;
        }

        return remoteAddr;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // Rate limit POST requests to auth endpoints
        if ("POST".equalsIgnoreCase(method) && path.startsWith("/api/auth/")) {
            return false; // Do not skip - apply filter
        }

        // Rate limit POST requests for pet creation
        if ("POST".equalsIgnoreCase(method) && path.equals("/api/pets")) {
            return false; // Do not skip - apply filter
        }

        // Rate limit POST requests for image uploads
        if ("POST".equalsIgnoreCase(method) && path.matches("/api/pets/[^/]+/images")) {
            return false; // Do not skip - apply filter
        }

        return true; // Skip filter for all other requests
    }

    /**
     * Check if an IP address is blocked.
     * Uses a cache to avoid hitting the database on every request.
     */
    private boolean isIpBlocked(String ip) {
        // Refresh cache if expired
        if (Instant.now().isAfter(blockedIpsCacheExpiry)) {
            refreshBlockedIpsCache();
        }
        return blockedIpsCache.contains(ip);
    }

    /**
     * Refresh the blocked IPs cache from the database.
     */
    private synchronized void refreshBlockedIpsCache() {
        if (Instant.now().isBefore(blockedIpsCacheExpiry)) {
            return; // Another thread already refreshed
        }

        try {
            List<String> blockedIps = jdbcClient.sql("""
                SELECT ip_address FROM blocked_ips
                WHERE ip_address IS NOT NULL
                AND active = true
                AND (permanent = true OR expires_at > NOW())
                """)
                    .query(String.class)
                    .list();

            blockedIpsCache = new HashSet<>(blockedIps);
            blockedIpsCacheExpiry = Instant.now().plus(BLOCKED_IPS_CACHE_TTL);
            logger.debug("Refreshed blocked IPs cache: {} entries", blockedIps.size());
        } catch (Exception e) {
            logger.error("Failed to refresh blocked IPs cache", e);
            // Keep using the old cache on error
            blockedIpsCacheExpiry = Instant.now().plus(Duration.ofSeconds(10));
        }
    }

    /**
     * Get rate limit status for all IPs (for admin dashboard).
     */
    public List<RateLimitStatus> getRateLimitStatus() {
        List<RateLimitStatus> statuses = new ArrayList<>();

        // Combine all bucket maps
        addBucketStatuses(statuses, loginBuckets, "login", 10);
        addBucketStatuses(statuses, registerBuckets, "register", 5);
        addBucketStatuses(statuses, passwordResetBuckets, "password_reset", 3);
        addBucketStatuses(statuses, petCreationBuckets, "pet_creation", 10);
        addBucketStatuses(statuses, imageUploadBuckets, "image_upload", 50);

        return statuses;
    }

    private void addBucketStatuses(List<RateLimitStatus> statuses, Map<String, Bucket> buckets,
                                    String type, int maxTokens) {
        buckets.forEach((ip, bucket) -> {
            long available = bucket.getAvailableTokens();
            if (available < maxTokens) { // Only show IPs that have consumed tokens
                statuses.add(new RateLimitStatus(
                        ip,
                        type,
                        (int) available,
                        maxTokens,
                        Instant.now().plusSeconds(60) // Approximate reset time
                ));
            }
        });
    }

    public record RateLimitStatus(
            String ip,
            String type,
            int remainingTokens,
            int maxTokens,
            Instant resetAt
    ) {}
}
