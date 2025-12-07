package com.example.foreverhome.logging;

import com.example.foreverhome.security.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter that sets up MDC context for each request.
 * This enables structured logging with user and request context.
 */
@Component
@Order(1)
public class LoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long startTime = System.currentTimeMillis();

        try {
            // Generate trace ID for this request
            String traceId = UUID.randomUUID().toString().substring(0, 8);
            MDC.put("traceId", traceId);

            // Add request info
            MDC.put("requestMethod", request.getMethod());
            MDC.put("requestPath", request.getRequestURI());

            // Get session ID if available
            String sessionId = request.getSession(false) != null
                ? request.getSession().getId().substring(0, 8)
                : null;
            if (sessionId != null) {
                MDC.put("sessionId", sessionId);
            }

            // Try to get user info from security context
            setUserContextFromSecurityContext();

            // Process the request
            filterChain.doFilter(request, response);

        } finally {
            // Log request completion with timing
            long duration = System.currentTimeMillis() - startTime;
            MDC.put("durationMs", String.valueOf(duration));
            MDC.put("responseStatus", String.valueOf(response.getStatus()));

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
}
