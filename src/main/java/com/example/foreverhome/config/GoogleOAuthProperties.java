package com.example.foreverhome.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for Google OAuth2 integration.
 */
@Configuration
@ConfigurationProperties(prefix = "google")
public class GoogleOAuthProperties {

    /**
     * Google OAuth2 Client ID from Google Cloud Console.
     */
    private String clientId;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    /**
     * Checks if Google OAuth is configured.
     * @return true if client ID is set
     */
    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank();
    }
}
