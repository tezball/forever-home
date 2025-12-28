package com.example.foreverhome.moderation;

import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Moderation Service - AI-powered content moderation for Forever Home pet profiles.
 *
 * Uses Spring AI with Ollama for local LLM inference:
 * - Text moderation (llama3.2): Checks pet names, descriptions, and health notes
 * - Image moderation (llava): Verifies images contain pets and are family-safe
 *
 * Access the web UI at http://localhost:8081/admin/moderation-queue
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class ModerationServiceApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(ModerationServiceApplication.class)
                .bannerMode(Banner.Mode.OFF)
                .logStartupInfo(false)
                .run(args);
    }
}
