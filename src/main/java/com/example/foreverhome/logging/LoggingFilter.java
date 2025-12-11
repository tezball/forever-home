package com.example.foreverhome.logging;

import com.example.foreverhome.security.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

/**
 * Filter that sets up MDC context for each request.
 * This enables structured logging with user and request context.
 */
@Component
@Order(1)
public class LoggingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);

    private static final Set<String> EXCLUDED_PATHS = Set.of(
        "/actuator/health",
        "/actuator/prometheus",
        "/swagger-ui",
        "/v3/api-docs"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long startTime = System.currentTimeMillis();
        String requestPath = request.getRequestURI();
        String method = request.getMethod();

        try {
            // Generate trace ID for this request
            String traceId = UUID.randomUUID().toString().substring(0, 8);
            MDC.put("traceId", traceId);

            // Add request info
            MDC.put("requestMethod", method);
            MDC.put("requestPath", requestPath);

            // Get client IP (handling proxy headers)
            String clientIp = getClientIp(request);
            MDC.put("clientIp", clientIp);

            // Get session ID if available
            String sessionId = request.getSession(false) != null
                ? request.getSession().getId().substring(0, 8)
                : null;
            if (sessionId != null) {
                MDC.put("sessionId", sessionId);
            }

            // Try to get user info from security context
            setUserContextFromSecurityContext();

            // Log incoming request (skip noisy paths)
            if (!isExcludedPath(requestPath)) {
                logger.debug("Incoming request: {} {} from {}", method, requestPath, clientIp);
            }

            // Process the request
            filterChain.doFilter(request, response);

        } finally {
            // Log request completion with timing
            long duration = System.currentTimeMillis() - startTime;
            MDC.put("durationMs", String.valueOf(duration));
            MDC.put("responseStatus", String.valueOf(response.getStatus()));

            // Log completed request (skip noisy paths)
            if (!isExcludedPath(requestPath)) {
                int status = response.getStatus();
                if (status >= 500) {
                    logger.error("Request completed: {} {} - {} ({}ms)", method, requestPath, status, duration);
                } else if (status >= 400) {
                    logger.warn("Request completed: {} {} - {} ({}ms)", method, requestPath, status, duration);
                } else if (duration > 1000) {
                    logger.warn("Slow request: {} {} - {} ({}ms)", method, requestPath, status, duration);
                } else {
                    logger.info("Request completed: {} {} - {} ({}ms)", method, requestPath, status, duration);
                }
            }

            // Clear MDC at end of request
            MDC.clear();
        }
    }

    private void setUserContextFromSecurityContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
            MDC.put("userId", principal.userId().toString());
            MDC.put("userRole", principal.role().name());
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    private boolean isExcludedPath(String path) {
        return EXCLUDED_PATHS.stream().anyMatch(path::startsWith);
    }
}
