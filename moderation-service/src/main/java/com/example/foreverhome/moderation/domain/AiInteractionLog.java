package com.example.foreverhome.moderation.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Log entry for AI model interactions (prompts and responses).
 */
public record AiInteractionLog(
        UUID id,
        Instant timestamp,
        UUID petId,
        ContentType contentType,
        String contentIdentifier,
        String modelUsed,
        String prompt,
        String response,
        long durationMs,
        boolean success,
        String errorMessage
) {
    public static AiInteractionLog success(UUID petId, ContentType contentType, String contentIdentifier,
                                            String modelUsed, String prompt, String response, long durationMs) {
        return new AiInteractionLog(
                UUID.randomUUID(),
                Instant.now(),
                petId,
                contentType,
                contentIdentifier,
                modelUsed,
                prompt,
                response,
                durationMs,
                true,
                null
        );
    }

    public static AiInteractionLog failure(UUID petId, ContentType contentType, String contentIdentifier,
                                            String modelUsed, String prompt, long durationMs, String errorMessage) {
        return new AiInteractionLog(
                UUID.randomUUID(),
                Instant.now(),
                petId,
                contentType,
                contentIdentifier,
                modelUsed,
                prompt,
                null,
                durationMs,
                false,
                errorMessage
        );
    }

    public String truncatedPrompt(int maxLength) {
        if (prompt == null) return "";
        return prompt.length() > maxLength ? prompt.substring(0, maxLength) + "..." : prompt;
    }

    public String truncatedResponse(int maxLength) {
        if (response == null) return "";
        return response.length() > maxLength ? response.substring(0, maxLength) + "..." : response;
    }
}
