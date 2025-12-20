package com.example.foreverhome.cli.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Forever Home CLI.
 */
@ConfigurationProperties(prefix = "fh")
public record CliProperties(
    String version,
    String name,
    int appPort,
    int postgresPort,
    int localstackPort,
    int grafanaPort,
    int lokiPort,
    int mailpitUiPort,
    int mailpitSmtpPort
) {
    public CliProperties {
        if (version == null) version = "0.0.1-SNAPSHOT";
        if (name == null) name = "Forever Home CLI";
        if (appPort == 0) appPort = 8080;
        if (postgresPort == 0) postgresPort = 5432;
        if (localstackPort == 0) localstackPort = 4566;
        if (grafanaPort == 0) grafanaPort = 3000;
        if (lokiPort == 0) lokiPort = 3100;
        if (mailpitUiPort == 0) mailpitUiPort = 8025;
        if (mailpitSmtpPort == 0) mailpitSmtpPort = 1025;
    }
}
