package com.example.foreverhome.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Validates critical security configuration on application startup.
 * Fails fast if security-sensitive settings are misconfigured in production.
 */
@Component
@Profile("prod")
public class SecurityConfigValidator {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfigValidator.class);
    private static final String DEFAULT_JWT_SECRET = "mySecretKeyForJWTTokenGenerationWhichMustBeLongEnough256BitsForHS256";
    private static final int MIN_JWT_SECRET_LENGTH = 32;

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Value("${app.security.trust-proxy:false}")
    private boolean trustProxy;

    @Value("${app.security.trusted-proxies:}")
    private String trustedProxies;

    @EventListener(ApplicationReadyEvent.class)
    public void validateSecurityConfig() {
        logger.info("Validating security configuration for production...");

        // Validate JWT secret
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new SecurityConfigurationException("JWT_SECRET environment variable must be set in production");
        }

        if (jwtSecret.equals(DEFAULT_JWT_SECRET)) {
            throw new SecurityConfigurationException(
                "JWT_SECRET is set to the default value. Please generate a secure random secret for production.");
        }

        if (jwtSecret.length() < MIN_JWT_SECRET_LENGTH) {
            throw new SecurityConfigurationException(
                "JWT_SECRET must be at least " + MIN_JWT_SECRET_LENGTH + " characters long");
        }

        // Warn about proxy configuration
        if (trustProxy && trustedProxies.isBlank()) {
            logger.warn("SECURITY WARNING: app.security.trust-proxy is enabled but no trusted proxies are configured. " +
                "This allows any client to spoof their IP address via X-Forwarded-For header. " +
                "Set app.security.trusted-proxies to your load balancer IP addresses.");
        }

        logger.info("Security configuration validation passed");
    }

    public static class SecurityConfigurationException extends RuntimeException {
        public SecurityConfigurationException(String message) {
            super(message);
        }
    }
}
