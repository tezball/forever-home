package com.example.foreverhome.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    private String allowedOrigins = "http://localhost:5173,http://localhost:3000";
    private String allowedMethods = "GET,POST,PUT,DELETE,OPTIONS";
    private boolean allowCredentials = true;
    private long maxAge = 3600;

    public List<String> getAllowedOriginsList() {
        return Arrays.asList(allowedOrigins.split(","));
    }

    public List<String> getAllowedMethodsList() {
        return Arrays.asList(allowedMethods.split(","));
    }

    // Getters and setters
    public String getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(String allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    public String getAllowedMethods() {
        return allowedMethods;
    }

    public void setAllowedMethods(String allowedMethods) {
        this.allowedMethods = allowedMethods;
    }

    public boolean isAllowCredentials() {
        return allowCredentials;
    }

    public void setAllowCredentials(boolean allowCredentials) {
        this.allowCredentials = allowCredentials;
    }

    public long getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(long maxAge) {
        this.maxAge = maxAge;
    }
}
