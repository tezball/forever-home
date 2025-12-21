package com.example.foreverhome.moderation.service;

import com.example.foreverhome.moderation.domain.*;
import com.example.foreverhome.moderation.repository.FlaggedContentRepository;
import com.example.foreverhome.moderation.repository.ModerationJobRepository;
import com.example.foreverhome.moderation.repository.ModerationResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for querying and managing moderation results.
 */
@Service
@Transactional
public class ModerationResultService {

    private static final Logger log = LoggerFactory.getLogger(ModerationResultService.class);

    private final ModerationResultRepository resultRepository;
    private final FlaggedContentRepository flagRepository;
    private final ModerationJobRepository jobRepository;

    public ModerationResultService(
            ModerationResultRepository resultRepository,
            FlaggedContentRepository flagRepository,
            ModerationJobRepository jobRepository) {
        this.resultRepository = resultRepository;
        this.flagRepository = flagRepository;
        this.jobRepository = jobRepository;
    }

    /**
     * Get all moderation results for a pet.
     */
    @Transactional(readOnly = true)
    public List<ModerationResult> getResultsForPet(UUID petId) {
        List<ModerationResult> results = resultRepository.findByPetIdOrderByCreatedAtDesc(petId);
        // Load flags for each result
        for (ModerationResult result : results) {
            List<FlaggedContent> flags = flagRepository.findByModerationResultId(result.getId());
            result.setFlags(flags);
        }
        return results;
    }

    /**
     * Get flagged results for review.
     */
    @Transactional(readOnly = true)
    public List<ModerationResult> getFlaggedResults(int limit) {
        List<ModerationResult> results = resultRepository.findFlaggedWithLimit(limit);
        // Load flags for each result
        for (ModerationResult result : results) {
            List<FlaggedContent> flags = flagRepository.findByModerationResultId(result.getId());
            result.setFlags(flags);
        }
        return results;
    }

    /**
     * Get flagged results by category.
     */
    @Transactional(readOnly = true)
    public List<ModerationResult> getFlaggedResultsByCategory(String category, int limit) {
        List<ModerationResult> results = resultRepository.findFlaggedByCategoryWithLimit(category, limit);
        // Load flags for each result
        for (ModerationResult result : results) {
            List<FlaggedContent> flags = flagRepository.findByModerationResultId(result.getId());
            result.setFlags(flags);
        }
        return results;
    }

    /**
     * Mark a moderation result as reviewed.
     */
    public ModerationResult review(UUID resultId, String action, String notes) {
        Optional<ModerationResult> resultOpt = resultRepository.findById(resultId);
        if (resultOpt.isEmpty()) {
            throw new IllegalArgumentException("Moderation result not found: " + resultId);
        }

        ModerationResult result = resultOpt.get();

        ModerationStatus newStatus = switch (action.toLowerCase()) {
            case "approve" -> ModerationStatus.APPROVED;
            case "reject" -> ModerationStatus.REJECTED;
            default -> throw new IllegalArgumentException("Invalid action: " + action + ". Use 'approve' or 'reject'");
        };

        // Use a placeholder UUID for reviewer (would come from auth in real system)
        result.markReviewed(UUID.randomUUID(), newStatus, notes);
        result.setIsNew(false);

        log.info("Moderation result {} marked as {} with notes: {}", resultId, newStatus, notes);

        return resultRepository.save(result);
    }

    /**
     * Get moderation statistics.
     */
    @Transactional(readOnly = true)
    public ModerationStats getStatistics() {
        long totalResults = resultRepository.count();
        long pendingCount = resultRepository.countByStatus("PENDING");
        long approvedCount = resultRepository.countByStatus("APPROVED");
        long flaggedCount = resultRepository.countByStatus("FLAGGED");
        long rejectedCount = resultRepository.countByStatus("REJECTED");

        long textResults = resultRepository.countByContentType("TEXT");
        long imageResults = resultRepository.countByContentType("IMAGE");

        long flaggedPets = resultRepository.countDistinctFlaggedPets();

        Long totalProcessed = jobRepository.sumProcessedPetsForCompletedJobs();
        Long totalFlagged = jobRepository.sumFlaggedPetsForCompletedJobs();

        long completedJobs = jobRepository.countByStatus("COMPLETED");
        long failedJobs = jobRepository.countByStatus("FAILED");

        return new ModerationStats(
                totalResults,
                pendingCount,
                approvedCount,
                flaggedCount,
                rejectedCount,
                textResults,
                imageResults,
                flaggedPets,
                totalProcessed != null ? totalProcessed : 0,
                totalFlagged != null ? totalFlagged : 0,
                completedJobs,
                failedJobs
        );
    }

    /**
     * Get recent jobs.
     */
    @Transactional(readOnly = true)
    public List<ModerationJob> getRecentJobs(int limit) {
        return jobRepository.findRecentJobs(limit);
    }

    /**
     * Moderation statistics record.
     */
    public record ModerationStats(
            long totalResults,
            long pendingCount,
            long approvedCount,
            long flaggedCount,
            long rejectedCount,
            long textResults,
            long imageResults,
            long flaggedPets,
            long totalProcessedPets,
            long totalFlaggedPets,
            long completedJobs,
            long failedJobs
    ) {
        public double flagRate() {
            if (totalResults == 0) return 0;
            return (double) flaggedCount / totalResults * 100;
        }

        public double approvalRate() {
            if (totalResults == 0) return 0;
            return (double) approvedCount / totalResults * 100;
        }
    }
}
