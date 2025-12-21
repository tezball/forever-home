package com.example.foreverhome.moderation.service;

import com.example.foreverhome.moderation.config.ModerationConfig;
import com.example.foreverhome.moderation.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for moderating text content using LLM.
 */
@Service
public class TextModerationService {

    private static final Logger log = LoggerFactory.getLogger(TextModerationService.class);

    private final OllamaChatModel chatModel;
    private final ModerationConfig config;

    private static final Pattern SCORE_PATTERN = Pattern.compile("SCORE:\\s*(\\d+\\.?\\d*)");
    private static final Pattern FLAG_PATTERN = Pattern.compile(
            "FLAG:\\s*(\\w+)\\s*-\\s*(LOW|MEDIUM|HIGH)\\s*-\\s*(.+)",
            Pattern.CASE_INSENSITIVE
    );

    public TextModerationService(OllamaChatModel chatModel, ModerationConfig config) {
        this.chatModel = chatModel;
        this.config = config;
        log.info("TextModerationService initialized - enabled={}, model={}, debug={}",
                config.textEnabled(), config.textModel(), config.debug());
    }

    /**
     * Check if text moderation is enabled.
     */
    public boolean isEnabled() {
        return config.textEnabled();
    }

    /**
     * Moderate a text field.
     */
    public ModerationResult moderateText(UUID petId, String fieldName, String content) {
        log.info("[TEXT] Starting moderation for pet={} field={}", petId, fieldName);

        if (!config.textEnabled()) {
            log.info("[TEXT] Text moderation is DISABLED - auto-approving pet={} field={}", petId, fieldName);
            return ModerationResult.createApproved(petId, ContentType.TEXT, fieldName, "disabled", 1.0);
        }

        if (content == null || content.isBlank()) {
            log.info("[TEXT] Skipping empty content for pet={} field={}", petId, fieldName);
            return ModerationResult.createApproved(petId, ContentType.TEXT, fieldName, "skip", 1.0);
        }

        Instant startTime = Instant.now();
        log.info("[TEXT] Analyzing pet={} field={} contentLength={}", petId, fieldName, content.length());

        try {
            String prompt = buildTextModerationPrompt(fieldName, content);

            if (config.debug()) {
                log.info("[TEXT][DEBUG] ========== PROMPT START ==========");
                log.info("[TEXT][DEBUG] Pet: {}", petId);
                log.info("[TEXT][DEBUG] Field: {}", fieldName);
                log.info("[TEXT][DEBUG] Content: {}", content);
                log.info("[TEXT][DEBUG] Full prompt:\n{}", prompt);
                log.info("[TEXT][DEBUG] ========== PROMPT END ==========");
            }

            log.debug("[TEXT] Calling LLM model={} temperature={}", config.textModel(), config.temperature());

            ChatResponse response = chatModel.call(new Prompt(prompt,
                    OllamaChatOptions.builder()
                            .model(config.textModel())
                            .temperature(config.temperature())
                            .build()));

            String responseText = response.getResult().getOutput().getText();
            Duration duration = Duration.between(startTime, Instant.now());

            log.info("[TEXT] LLM responded in {}ms for pet={} field={}", duration.toMillis(), petId, fieldName);

            if (config.debug()) {
                log.info("[TEXT][DEBUG] ========== RESPONSE START ==========");
                log.info("[TEXT][DEBUG] Raw response:\n{}", responseText);
                log.info("[TEXT][DEBUG] ========== RESPONSE END ==========");
            }

            ModerationResult result = parseTextModerationResponse(petId, fieldName, responseText);

            log.info("[TEXT] Moderation complete: pet={} field={} status={} confidence={} flags={} duration={}ms",
                    petId, fieldName, result.getStatus(), result.getConfidenceScore(),
                    result.getFlags() != null ? result.getFlags().size() : 0, duration.toMillis());

            return result;

        } catch (Exception e) {
            Duration duration = Duration.between(startTime, Instant.now());
            log.error("[TEXT] Error moderating pet={} field={} after {}ms: {}",
                    petId, fieldName, duration.toMillis(), e.getMessage(), e);
            return ModerationResult.createPending(petId, ContentType.TEXT, fieldName,
                    config.textModel(), e.getMessage());
        }
    }

    /**
     * Moderate all text content for a pet.
     */
    public List<ModerationResult> moderatePetTextContent(UUID petId, String name,
                                                          String description, String healthNotes) {
        log.info("[TEXT] ===== Starting text moderation for pet={} =====", petId);
        Instant startTime = Instant.now();

        List<ModerationResult> results = new ArrayList<>();

        results.add(moderateText(petId, "name", name));
        results.add(moderateText(petId, "description", description));
        results.add(moderateText(petId, "healthNotes", healthNotes));

        Duration totalDuration = Duration.between(startTime, Instant.now());
        long flaggedCount = results.stream()
                .filter(r -> r.getStatus() == ModerationStatus.FLAGGED)
                .count();

        log.info("[TEXT] ===== Completed text moderation for pet={}: {} results, {} flagged, {}ms total =====",
                petId, results.size(), flaggedCount, totalDuration.toMillis());

        return results;
    }

    private String buildTextModerationPrompt(String fieldName, String content) {
        return """
                You are a content moderator for a pet adoption platform called Forever Home.
                Your task is to analyze the following %s field content for a pet profile.

                CONTENT TO ANALYZE:
                "%s"

                CHECK FOR THE FOLLOWING ISSUES:
                1. INAPPROPRIATE - Profanity, offensive language, or adult content
                2. MISLEADING - False claims about pet health, breed, or characteristics
                3. SPAM - Advertising, promotional content, or irrelevant information
                4. SCAM - Requests for money, suspicious contact information, or fraud indicators
                5. HARMFUL - Content promoting animal abuse or neglect

                RESPOND IN THIS EXACT FORMAT:
                SCORE: [0.0 to 1.0 confidence that content is acceptable]

                If there are issues, add lines in this format:
                FLAG: [CATEGORY] - [SEVERITY: LOW/MEDIUM/HIGH] - [Description of the issue]

                If content is acceptable, just provide the SCORE line.

                EXAMPLES:
                For acceptable content: "SCORE: 0.95"
                For flagged content:
                "SCORE: 0.3
                FLAG: INAPPROPRIATE - HIGH - Contains profanity in the description
                FLAG: SPAM - MEDIUM - Includes promotional website link"
                """.formatted(fieldName, content);
    }

    private ModerationResult parseTextModerationResponse(UUID petId, String fieldName, String response) {
        List<FlaggedContent> flags = new ArrayList<>();
        double confidenceScore = 0.5;

        // Extract confidence score
        Matcher scoreMatcher = SCORE_PATTERN.matcher(response);
        if (scoreMatcher.find()) {
            try {
                confidenceScore = Double.parseDouble(scoreMatcher.group(1));
                // Clamp to 0-1 range
                confidenceScore = Math.max(0, Math.min(1, confidenceScore));
            } catch (NumberFormatException e) {
                log.warn("Could not parse score from response: {}", response);
            }
        }

        // Extract flags
        Matcher flagMatcher = FLAG_PATTERN.matcher(response);
        while (flagMatcher.find()) {
            String categoryStr = flagMatcher.group(1).toUpperCase();
            String severityStr = flagMatcher.group(2).toUpperCase();
            String description = flagMatcher.group(3).trim();

            try {
                ModerationCategory category = parseCategory(categoryStr);
                Severity severity = Severity.valueOf(severityStr);

                FlaggedContent flag = FlaggedContent.create(
                        category,
                        severity,
                        description,
                        suggestAction(severity)
                );
                flags.add(flag);
            } catch (IllegalArgumentException e) {
                log.warn("Could not parse flag: {} - {} - {}", categoryStr, severityStr, description);
            }
        }

        if (flags.isEmpty()) {
            return ModerationResult.createApproved(petId, ContentType.TEXT, fieldName,
                    config.textModel(), confidenceScore);
        } else {
            ModerationResult result = ModerationResult.createFlagged(petId, ContentType.TEXT, fieldName,
                    config.textModel(), confidenceScore, response);
            for (FlaggedContent flag : flags) {
                flag.setModerationResultId(result.getId());
                result.addFlag(flag);
            }
            return result;
        }
    }

    private ModerationCategory parseCategory(String categoryStr) {
        return switch (categoryStr) {
            case "INAPPROPRIATE" -> ModerationCategory.INAPPROPRIATE;
            case "MISLEADING" -> ModerationCategory.MISLEADING;
            case "SPAM" -> ModerationCategory.SPAM;
            case "SCAM" -> ModerationCategory.SCAM;
            case "HARMFUL" -> ModerationCategory.HARMFUL;
            default -> ModerationCategory.OTHER;
        };
    }

    private String suggestAction(Severity severity) {
        return switch (severity) {
            case LOW -> "REVIEW";
            case MEDIUM -> "WARN_USER";
            case HIGH -> "REMOVE";
        };
    }
}
