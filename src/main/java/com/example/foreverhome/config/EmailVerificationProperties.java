package com.example.foreverhome.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.email.verification")
public record EmailVerificationProperties(
        boolean autoActivate,
        boolean useConsole
) {
    public EmailVerificationProperties {
        // Defaults applied via application.properties
    }
}
