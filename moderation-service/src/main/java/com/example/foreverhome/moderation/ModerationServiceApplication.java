package com.example.foreverhome.moderation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Moderation Service - AI-powered content moderation for Forever Home pet profiles.
 *
 * Uses Spring AI with Ollama for local LLM inference:
 * - Text moderation (llama3.2): Checks pet names, descriptions, and health notes
 * - Image moderation (llava): Verifies images contain pets and are family-safe
 *
 * Supports both interactive REPL mode and single-command execution:
 * - Interactive: `moderation-service` (enters shell)
 * - Single command: `moderation-service moderate pet --pet-id <UUID>`
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class ModerationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ModerationServiceApplication.class, args);
    }
}
