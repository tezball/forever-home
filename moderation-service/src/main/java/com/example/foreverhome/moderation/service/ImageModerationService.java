package com.example.foreverhome.moderation.service;

import com.example.foreverhome.moderation.client.ForeverHomeApiClient;
import com.example.foreverhome.moderation.config.ModerationConfig;
import com.example.foreverhome.moderation.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.content.Media;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for moderating image content using vision LLM.
 */
@Service
public class ImageModerationService {

    private static final Logger log = LoggerFactory.getLogger(ImageModerationService.class);

    private final OllamaChatModel chatModel;
    private final ForeverHomeApiClient apiClient;
    private final ModerationConfig config;
    private final AiInteractionLogService aiLogService;

    private static final Pattern IS_PET_PATTERN = Pattern.compile("IS_PET:\\s*(YES|NO)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SAFE_PATTERN = Pattern.compile("FAMILY_SAFE:\\s*(YES|NO)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ISSUE_PATTERN = Pattern.compile(
            "ISSUE:\\s*(\\w+)\\s*-\\s*(.+)",
            Pattern.CASE_INSENSITIVE
    );

    public ImageModerationService(OllamaChatModel chatModel, ForeverHomeApiClient apiClient,
                                   ModerationConfig config, AiInteractionLogService aiLogService) {
        this.chatModel = chatModel;
        this.apiClient = apiClient;
        this.config = config;
        this.aiLogService = aiLogService;
        log.info("ImageModerationService initialized - enabled={}, model={}, debug={}",
                config.imageEnabled(), config.visionModel(), config.debug());
    }

    /**
     * Check if image moderation is enabled.
     */
    public boolean isEnabled() {
        return config.imageEnabled();
    }

    /**
     * Moderate a single image.
     */
    public ModerationResult moderateImage(UUID petId, UUID jobId, String imageUrl) {
        log.info("[IMAGE] Starting moderation for pet={} url={}", petId, truncateUrl(imageUrl));

        if (!config.imageEnabled()) {
            log.info("[IMAGE] Image moderation is DISABLED - auto-approving pet={}", petId);
            return ModerationResult.createApproved(petId, jobId, ContentType.IMAGE, imageUrl, "disabled", 1.0);
        }

        Instant startTime = Instant.now();

        try {
            // Fetch image bytes
            log.info("[IMAGE] Fetching image bytes for pet={}", petId);
            Instant fetchStart = Instant.now();
            byte[] imageBytes = apiClient.fetchImageBytes(imageUrl);
            Duration fetchDuration = Duration.between(fetchStart, Instant.now());
            String mimeType = detectMimeType(imageUrl);

            log.info("[IMAGE] Fetched image: pet={} size={}KB mimeType={} fetchTime={}ms",
                    petId, imageBytes.length / 1024, mimeType, fetchDuration.toMillis());

            if (config.debug()) {
                log.info("[IMAGE][DEBUG] ========== IMAGE INFO ==========");
                log.info("[IMAGE][DEBUG] Pet: {}", petId);
                log.info("[IMAGE][DEBUG] URL: {}", imageUrl);
                log.info("[IMAGE][DEBUG] Size: {} bytes", imageBytes.length);
                log.info("[IMAGE][DEBUG] MIME Type: {}", mimeType);
                log.info("[IMAGE][DEBUG] ========== END IMAGE INFO ==========");
            }

            // Create multimodal message with image
            var imageResource = new ByteArrayResource(imageBytes);
            var media = new Media(MimeTypeUtils.parseMimeType(mimeType), imageResource);
            String promptText = buildImageModerationPrompt();
            var userMessage = UserMessage.builder()
                    .text(promptText)
                    .media(media)
                    .build();

            if (config.debug()) {
                log.info("[IMAGE][DEBUG] ========== PROMPT START ==========");
                log.info("[IMAGE][DEBUG] Prompt:\n{}", promptText);
                log.info("[IMAGE][DEBUG] ========== PROMPT END ==========");
            }

            // Call vision model
            log.debug("[IMAGE] Calling vision LLM model={} temperature={}", config.visionModel(), config.temperature());
            Instant llmStart = Instant.now();

            ChatResponse response = chatModel.call(new Prompt(userMessage,
                    OllamaChatOptions.builder()
                            .model(config.visionModel())
                            .temperature(config.temperature())
                            .build()));

            String responseText = response.getResult().getOutput().getText();
            Duration llmDuration = Duration.between(llmStart, Instant.now());

            log.info("[IMAGE] Vision LLM responded in {}ms for pet={}", llmDuration.toMillis(), petId);

            // Log AI interaction
            aiLogService.logSuccess(petId, ContentType.IMAGE, truncateUrl(imageUrl), config.visionModel(),
                    promptText, responseText, llmDuration.toMillis());

            if (config.debug()) {
                log.info("[IMAGE][DEBUG] ========== RESPONSE START ==========");
                log.info("[IMAGE][DEBUG] Raw response:\n{}", responseText);
                log.info("[IMAGE][DEBUG] ========== RESPONSE END ==========");
            }

            ModerationResult result = parseImageModerationResponse(petId, jobId, imageUrl, responseText);
            Duration totalDuration = Duration.between(startTime, Instant.now());

            log.info("[IMAGE] Moderation complete: pet={} status={} confidence={} flags={} totalTime={}ms (fetch={}ms, llm={}ms)",
                    petId, result.getStatus(), result.getConfidenceScore(),
                    result.getFlags() != null ? result.getFlags().size() : 0,
                    totalDuration.toMillis(), fetchDuration.toMillis(), llmDuration.toMillis());

            return result;

        } catch (Exception e) {
            Duration duration = Duration.between(startTime, Instant.now());
            log.error("[IMAGE] Error moderating pet={} after {}ms: {}", petId, duration.toMillis(), e.getMessage(), e);

            // Log AI interaction failure
            aiLogService.logFailure(petId, ContentType.IMAGE, truncateUrl(imageUrl), config.visionModel(),
                    null, duration.toMillis(), e.getMessage());

            return ModerationResult.createPending(petId, jobId, ContentType.IMAGE, imageUrl,
                    config.visionModel(), e.getMessage());
        }
    }

    private String truncateUrl(String url) {
        if (url == null) return "null";
        return url.length() > 80 ? url.substring(0, 77) + "..." : url;
    }

    /**
     * Moderate all images for a pet.
     */
    public List<ModerationResult> moderatePetImages(UUID petId, UUID jobId, List<String> imageUrls) {
        log.info("[IMAGE] ===== Starting image moderation for pet={} imageCount={} =====",
                petId, imageUrls != null ? imageUrls.size() : 0);

        if (imageUrls == null || imageUrls.isEmpty()) {
            log.info("[IMAGE] No images to moderate for pet={}", petId);
            return List.of();
        }

        Instant startTime = Instant.now();
        List<ModerationResult> results = new ArrayList<>();

        for (int i = 0; i < imageUrls.size(); i++) {
            log.info("[IMAGE] Processing image {}/{} for pet={}", i + 1, imageUrls.size(), petId);
            results.add(moderateImage(petId, jobId, imageUrls.get(i)));
        }

        Duration totalDuration = Duration.between(startTime, Instant.now());
        long flaggedCount = results.stream()
                .filter(r -> r.getStatus() == ModerationStatus.FLAGGED)
                .count();

        log.info("[IMAGE] ===== Completed image moderation for pet={}: {} results, {} flagged, {}ms total =====",
                petId, results.size(), flaggedCount, totalDuration.toMillis());

        return results;
    }

    private String buildImageModerationPrompt() {
        return """
                You are a content moderator for a pet adoption platform called Forever Home.
                Analyze this image that was uploaded as a pet profile photo.

                CHECK THE FOLLOWING:
                1. Does this image contain a pet (dog or cat)?
                2. Is the image family-friendly and appropriate for all ages?
                3. Is the image clear enough to see the pet?
                4. Are there any concerning elements (violence, inappropriate content, etc.)?

                RESPOND IN THIS EXACT FORMAT:
                IS_PET: [YES/NO]
                FAMILY_SAFE: [YES/NO]

                If there are specific issues, add lines:
                ISSUE: [CATEGORY] - [Description]

                Categories: NOT_PET, NSFW, UNCLEAR, MISLEADING, INAPPROPRIATE

                EXAMPLE RESPONSES:

                For a good pet photo:
                "IS_PET: YES
                FAMILY_SAFE: YES"

                For a problematic image:
                "IS_PET: NO
                FAMILY_SAFE: YES
                ISSUE: NOT_PET - Image shows a car instead of a pet"
                """;
    }

    private ModerationResult parseImageModerationResponse(UUID petId, UUID jobId, String imageUrl, String response) {
        List<FlaggedContent> flags = new ArrayList<>();
        double confidenceScore = 0.8;

        // Check if image contains a pet
        Matcher petMatcher = IS_PET_PATTERN.matcher(response);
        boolean isPet = true;
        if (petMatcher.find()) {
            isPet = "YES".equalsIgnoreCase(petMatcher.group(1));
            if (!isPet) {
                FlaggedContent flag = FlaggedContent.create(
                        ModerationCategory.NOT_PET,
                        Severity.HIGH,
                        "Image does not appear to contain a pet",
                        "REMOVE"
                );
                flags.add(flag);
                confidenceScore = 0.9;
            }
        }

        // Check if image is family-safe
        Matcher safeMatcher = SAFE_PATTERN.matcher(response);
        if (safeMatcher.find()) {
            boolean isSafe = "YES".equalsIgnoreCase(safeMatcher.group(1));
            if (!isSafe) {
                FlaggedContent flag = FlaggedContent.create(
                        ModerationCategory.NSFW,
                        Severity.HIGH,
                        "Image flagged as not family-safe",
                        "REMOVE"
                );
                flags.add(flag);
                confidenceScore = 0.95;
            }
        }

        // Parse additional issues
        Matcher issueMatcher = ISSUE_PATTERN.matcher(response);
        while (issueMatcher.find()) {
            String categoryStr = issueMatcher.group(1).toUpperCase();
            String description = issueMatcher.group(2).trim();

            ModerationCategory category = parseCategory(categoryStr);
            // Skip if we already added this category
            if (flags.stream().anyMatch(f -> f.getCategory() == category)) {
                continue;
            }

            FlaggedContent flag = FlaggedContent.create(
                    category,
                    category == ModerationCategory.NOT_PET || category == ModerationCategory.NSFW
                            ? Severity.HIGH : Severity.MEDIUM,
                    description,
                    category == ModerationCategory.NOT_PET || category == ModerationCategory.NSFW
                            ? "REMOVE" : "REVIEW"
            );
            flags.add(flag);
        }

        if (flags.isEmpty()) {
            return ModerationResult.createApproved(petId, jobId, ContentType.IMAGE, imageUrl,
                    config.visionModel(), confidenceScore);
        } else {
            ModerationResult result = ModerationResult.createFlagged(petId, jobId, ContentType.IMAGE, imageUrl,
                    config.visionModel(), confidenceScore, response);
            for (FlaggedContent flag : flags) {
                flag.setModerationResultId(result.getId());
                result.addFlag(flag);
            }
            return result;
        }
    }

    private ModerationCategory parseCategory(String categoryStr) {
        return switch (categoryStr) {
            case "NOT_PET" -> ModerationCategory.NOT_PET;
            case "NSFW" -> ModerationCategory.NSFW;
            case "UNCLEAR" -> ModerationCategory.UNCLEAR;
            case "MISLEADING" -> ModerationCategory.MISLEADING;
            case "INAPPROPRIATE" -> ModerationCategory.INAPPROPRIATE;
            default -> ModerationCategory.OTHER;
        };
    }

    private String detectMimeType(String url) {
        String lower = url.toLowerCase();
        if (lower.contains(".png")) return "image/png";
        if (lower.contains(".gif")) return "image/gif";
        if (lower.contains(".webp")) return "image/webp";
        return "image/jpeg";  // Default
    }
}
