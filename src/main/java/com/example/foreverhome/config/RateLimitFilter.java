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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting filter for authentication endpoints to prevent brute force attacks.
 *
 * Limits:
 * - Login: 10 requests per minute per IP
 * - Register: 5 requests per minute per IP
 * - Password reset: 3 requests per minute per IP
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitFilter.class);

    // Different buckets for different endpoint types
    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> registerBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> passwordResetBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> resendVerificationBuckets = new ConcurrentHashMap<>();
    private final MetricsService metricsService;

    public RateLimitFilter(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String clientIp = getClientIp(request);

        // Only rate limit auth endpoints
        if (path.startsWith("/api/auth/")) {
            Bucket bucket = getBucketForRequest(path, clientIp);

            if (bucket != null && !bucket.tryConsume(1)) {
                logger.warn("Rate limit exceeded for {} from IP {}", path, clientIp);
                metricsService.recordRateLimitHit(path, clientIp);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Too many requests. Please try again later.\",\"retryAfter\":60}");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private Bucket getBucketForRequest(String path, String clientIp) {
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

    private String getClientIp(HttpServletRequest request) {
        // Check for proxied requests
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // Take the first IP in the chain (original client)
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Only filter POST requests to auth endpoints
        String path = request.getRequestURI();
        String method = request.getMethod();

        // Rate limit POST requests to auth endpoints
        if ("POST".equalsIgnoreCase(method) && path.startsWith("/api/auth/")) {
            return false; // Do not skip - apply filter
        }

        return true; // Skip filter for all other requests
    }
}
