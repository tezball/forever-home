package com.example.foreverhome.moderation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for moderation service.
 *
 * Feature flags:
 * - textEnabled: Enable/disable text content moderation (name, description, health notes)
 * - imageEnabled: Enable/disable image moderation (pet photos)
 * - debug: Enable verbose logging of prompts and LLM responses
 */
@ConfigurationProperties(prefix = "moderation")
public record ModerationConfig(
        String textModel,
        String visionModel,
        double temperature,
        boolean textEnabled,
        boolean imageEnabled,
        boolean debug
) {
    public ModerationConfig {
        if (textModel == null || textModel.isBlank()) {
            textModel = "llama3.2";
        }
        if (visionModel == null || visionModel.isBlank()) {
            visionModel = "llava";
        }
        if (temperature <= 0) {
            temperature = 0.1;
        }
        // Note: Boolean primitives default to false when not set by Spring,
        // so we use the explicit defaulting in application.properties
    }
}
