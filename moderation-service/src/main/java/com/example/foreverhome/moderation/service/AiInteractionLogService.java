package com.example.foreverhome.moderation.service;

import com.example.foreverhome.moderation.domain.AiInteractionLog;
import com.example.foreverhome.moderation.domain.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Service for storing and retrieving AI interaction logs.
 * Uses in-memory storage with a configurable max size.
 */
@Service
public class AiInteractionLogService {

    private static final Logger log = LoggerFactory.getLogger(AiInteractionLogService.class);
    private static final int MAX_LOGS = 500;

    private final ConcurrentLinkedDeque<AiInteractionLog> logs = new ConcurrentLinkedDeque<>();

    /**
     * Log a successful AI interaction.
     */
    public void logSuccess(UUID petId, ContentType contentType, String contentIdentifier,
                           String modelUsed, String prompt, String response, long durationMs) {
        AiInteractionLog entry = AiInteractionLog.success(
                petId, contentType, contentIdentifier, modelUsed, prompt, response, durationMs);
        addLog(entry);
        log.info("[AI-LOG] SUCCESS pet={} type={} model={} duration={}ms",
                petId, contentType, modelUsed, durationMs);
    }

    /**
     * Log a failed AI interaction.
     */
    public void logFailure(UUID petId, ContentType contentType, String contentIdentifier,
                           String modelUsed, String prompt, long durationMs, String errorMessage) {
        AiInteractionLog entry = AiInteractionLog.failure(
                petId, contentType, contentIdentifier, modelUsed, prompt, durationMs, errorMessage);
        addLog(entry);
        log.warn("[AI-LOG] FAILURE pet={} type={} model={} duration={}ms error={}",
                petId, contentType, modelUsed, durationMs, errorMessage);
    }

    private void addLog(AiInteractionLog entry) {
        logs.addFirst(entry);
        // Trim to max size
        while (logs.size() > MAX_LOGS) {
            logs.removeLast();
        }
    }

    /**
     * Get recent logs (most recent first).
     */
    public List<AiInteractionLog> getRecentLogs(int limit) {
        List<AiInteractionLog> result = new ArrayList<>();
        int count = 0;
        for (AiInteractionLog entry : logs) {
            if (count >= limit) break;
            result.add(entry);
            count++;
        }
        return result;
    }

    /**
     * Get logs filtered by content type.
     */
    public List<AiInteractionLog> getLogsByContentType(ContentType contentType, int limit) {
        List<AiInteractionLog> result = new ArrayList<>();
        for (AiInteractionLog entry : logs) {
            if (result.size() >= limit) break;
            if (entry.contentType() == contentType) {
                result.add(entry);
            }
        }
        return result;
    }

    /**
     * Get logs for a specific pet.
     */
    public List<AiInteractionLog> getLogsForPet(UUID petId, int limit) {
        List<AiInteractionLog> result = new ArrayList<>();
        for (AiInteractionLog entry : logs) {
            if (result.size() >= limit) break;
            if (entry.petId().equals(petId)) {
                result.add(entry);
            }
        }
        return result;
    }

    /**
     * Get a specific log by ID.
     */
    public AiInteractionLog getLog(UUID logId) {
        for (AiInteractionLog entry : logs) {
            if (entry.id().equals(logId)) {
                return entry;
            }
        }
        return null;
    }

    /**
     * Get statistics about logged interactions.
     */
    public LogStats getStats() {
        long total = logs.size();
        long successful = logs.stream().filter(AiInteractionLog::success).count();
        long failed = total - successful;
        long textCount = logs.stream().filter(l -> l.contentType() == ContentType.TEXT).count();
        long imageCount = logs.stream().filter(l -> l.contentType() == ContentType.IMAGE).count();
        double avgDuration = logs.stream().mapToLong(AiInteractionLog::durationMs).average().orElse(0);
        return new LogStats(total, successful, failed, textCount, imageCount, avgDuration);
    }

    /**
     * Clear all logs.
     */
    public void clearLogs() {
        logs.clear();
        log.info("[AI-LOG] All logs cleared");
    }

    /**
     * Statistics record for AI interaction logs.
     */
    public record LogStats(
            long totalLogs,
            long successfulLogs,
            long failedLogs,
            long textLogs,
            long imageLogs,
            double avgDurationMs
    ) {}
}
