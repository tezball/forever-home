package com.example.foreverhome.moderation.service;

import com.example.foreverhome.moderation.client.ForeverHomeApiClient;
import com.example.foreverhome.moderation.client.dto.PetProfileDto;
import com.example.foreverhome.moderation.config.ModerationConfig;
import com.example.foreverhome.moderation.domain.*;
import com.example.foreverhome.moderation.repository.FlaggedContentRepository;
import com.example.foreverhome.moderation.repository.ModerationJobRepository;
import com.example.foreverhome.moderation.repository.ModerationResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Orchestrates the moderation of pet profiles.
 */
@Service
@Transactional
public class PetModerationOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(PetModerationOrchestrator.class);

    private final ForeverHomeApiClient apiClient;
    private final TextModerationService textModerationService;
    private final ImageModerationService imageModerationService;
    private final ModerationResultRepository resultRepository;
    private final FlaggedContentRepository flagRepository;
    private final ModerationJobRepository jobRepository;
    private final ModerationConfig config;

    public PetModerationOrchestrator(
            ForeverHomeApiClient apiClient,
            TextModerationService textModerationService,
            ImageModerationService imageModerationService,
            ModerationResultRepository resultRepository,
            FlaggedContentRepository flagRepository,
            ModerationJobRepository jobRepository,
            ModerationConfig config) {
        this.apiClient = apiClient;
        this.textModerationService = textModerationService;
        this.imageModerationService = imageModerationService;
        this.resultRepository = resultRepository;
        this.flagRepository = flagRepository;
        this.jobRepository = jobRepository;
        this.config = config;

        log.info("[ORCHESTRATOR] Initialized - textEnabled={}, imageEnabled={}, debug={}",
                config.textEnabled(), config.imageEnabled(), config.debug());
    }

    /**
     * Moderate a single pet by ID.
     */
    public List<ModerationResult> moderatePet(UUID petId) {
        return moderatePet(petId, false, false);
    }

    /**
     * Moderate a single pet by ID with options.
     */
    public List<ModerationResult> moderatePet(UUID petId, boolean textOnly, boolean imagesOnly) {
        log.info("[ORCHESTRATOR] Starting moderation for pet={} textOnly={} imagesOnly={}",
                petId, textOnly, imagesOnly);

        Optional<PetProfileDto> petOpt = apiClient.getPet(petId);
        if (petOpt.isEmpty()) {
            log.warn("[ORCHESTRATOR] Pet {} not found - cannot moderate", petId);
            return List.of();
        }

        return moderatePetProfile(petOpt.get(), textOnly, imagesOnly);
    }

    /**
     * Moderate a pet profile.
     */
    public List<ModerationResult> moderatePetProfile(PetProfileDto pet, boolean textOnly, boolean imagesOnly) {
        log.info("[ORCHESTRATOR] ========== MODERATING PET: {} ({}) ==========", pet.name(), pet.id());
        log.info("[ORCHESTRATOR] Options: textOnly={}, imagesOnly={}", textOnly, imagesOnly);
        log.info("[ORCHESTRATOR] Config: textEnabled={}, imageEnabled={}", config.textEnabled(), config.imageEnabled());

        Instant startTime = Instant.now();
        List<ModerationResult> results = new ArrayList<>();

        // Determine what to moderate based on flags and config
        boolean shouldModerateText = !imagesOnly && pet.hasTextContent() && config.textEnabled();
        boolean shouldModerateImages = !textOnly && pet.hasImages() && config.imageEnabled();

        log.info("[ORCHESTRATOR] Will moderate: text={} (hasContent={}, enabled={}), images={} (hasImages={}, enabled={})",
                shouldModerateText, pet.hasTextContent(), config.textEnabled(),
                shouldModerateImages, pet.hasImages(), config.imageEnabled());

        // Moderate text content
        if (!imagesOnly && pet.hasTextContent()) {
            if (!config.textEnabled()) {
                log.info("[ORCHESTRATOR] Skipping text moderation - DISABLED by config");
            }
            List<ModerationResult> textResults = textModerationService.moderatePetTextContent(
                    pet.id(), pet.name(), pet.description(), pet.healthNotes());
            results.addAll(saveResults(textResults));
        }

        // Moderate images
        if (!textOnly && pet.hasImages()) {
            if (!config.imageEnabled()) {
                log.info("[ORCHESTRATOR] Skipping image moderation - DISABLED by config");
            }
            List<ModerationResult> imageResults = imageModerationService.moderatePetImages(
                    pet.id(), pet.imageUrls());
            results.addAll(saveResults(imageResults));
        }

        Duration totalDuration = Duration.between(startTime, Instant.now());
        long flaggedCount = results.stream()
                .filter(r -> r.getStatus() == ModerationStatus.FLAGGED)
                .count();

        log.info("[ORCHESTRATOR] ========== COMPLETED PET: {} ==========", pet.id());
        log.info("[ORCHESTRATOR] Results: {} total, {} flagged, {}ms duration",
                results.size(), flaggedCount, totalDuration.toMillis());

        return results;
    }

    /**
     * Run batch moderation on available pets.
     */
    public ModerationJob runBatch(int limit, boolean textOnly, boolean imagesOnly,
                                   Consumer<String> progressCallback) {
        log.info("[BATCH] ========================================");
        log.info("[BATCH] STARTING BATCH MODERATION");
        log.info("[BATCH] ========================================");
        log.info("[BATCH] Parameters: limit={}, textOnly={}, imagesOnly={}", limit, textOnly, imagesOnly);
        log.info("[BATCH] Config: textEnabled={}, imageEnabled={}, debug={}",
                config.textEnabled(), config.imageEnabled(), config.debug());

        Instant batchStart = Instant.now();

        // Fetch pets to moderate
        log.info("[BATCH] Fetching available pets from API...");
        List<PetProfileDto> pets = apiClient.getAllAvailablePets(limit);
        log.info("[BATCH] Found {} pets to moderate", pets.size());

        if (pets.isEmpty()) {
            log.info("[BATCH] No pets to moderate - exiting");
            return null;
        }

        // Create job
        ModerationJob job = ModerationJob.create(pets.size(), textOnly, imagesOnly);
        job = jobRepository.save(job);
        job.start();
        job = jobRepository.save(job);
        log.info("[BATCH] Created job: {}", job.getId());

        try {
            for (int i = 0; i < pets.size(); i++) {
                PetProfileDto pet = pets.get(i);
                Instant petStart = Instant.now();

                String progress = String.format("[BATCH] Processing pet %d/%d: %s (%s)",
                        i + 1, pets.size(), pet.name(), pet.id());
                log.info(progress);

                if (progressCallback != null) {
                    progressCallback.accept(progress);
                }

                List<ModerationResult> results = moderatePetProfile(pet, textOnly, imagesOnly);

                boolean wasFlagged = results.stream()
                        .anyMatch(r -> r.getStatus() == ModerationStatus.FLAGGED);
                job.recordProgress(wasFlagged);

                Duration petDuration = Duration.between(petStart, Instant.now());
                log.info("[BATCH] Pet {} complete: {} results, flagged={}, {}ms",
                        pet.id(), results.size(), wasFlagged, petDuration.toMillis());

                // Save job progress periodically
                if ((i + 1) % 10 == 0) {
                    job.setIsNew(false);
                    jobRepository.save(job);
                    log.info("[BATCH] Checkpoint saved: {}/{} processed, {} flagged",
                            job.getProcessedPets(), job.getTotalPets(), job.getFlaggedPets());
                }
            }

            job.complete();
            job.setIsNew(false);
            job = jobRepository.save(job);

            Duration batchDuration = Duration.between(batchStart, Instant.now());
            double avgTimePerPet = pets.isEmpty() ? 0 : batchDuration.toMillis() / (double) pets.size();

            log.info("[BATCH] ========================================");
            log.info("[BATCH] BATCH MODERATION COMPLETED");
            log.info("[BATCH] ========================================");
            log.info("[BATCH] Job ID: {}", job.getId());
            log.info("[BATCH] Processed: {}/{} pets", job.getProcessedPets(), job.getTotalPets());
            log.info("[BATCH] Flagged: {} ({}%)", job.getFlaggedPets(),
                    job.getProcessedPets() > 0 ? (job.getFlaggedPets() * 100.0 / job.getProcessedPets()) : 0);
            log.info("[BATCH] Duration: {}ms (avg {}ms/pet)", batchDuration.toMillis(), (long) avgTimePerPet);

        } catch (Exception e) {
            Duration batchDuration = Duration.between(batchStart, Instant.now());
            log.error("[BATCH] ========================================");
            log.error("[BATCH] BATCH MODERATION FAILED after {}ms", batchDuration.toMillis());
            log.error("[BATCH] ========================================");
            log.error("[BATCH] Error: {}", e.getMessage(), e);
            job.fail(e.getMessage());
            job.setIsNew(false);
            jobRepository.save(job);
        }

        return job;
    }

    /**
     * Save moderation results and their flags.
     */
    private List<ModerationResult> saveResults(List<ModerationResult> results) {
        List<ModerationResult> savedResults = new ArrayList<>();

        for (ModerationResult result : results) {
            // Save the result
            ModerationResult savedResult = resultRepository.save(result);
            savedResult.setIsNew(false);

            // Save flags
            List<FlaggedContent> savedFlags = new ArrayList<>();
            for (FlaggedContent flag : result.getFlags()) {
                flag.setModerationResultId(savedResult.getId());
                FlaggedContent savedFlag = flagRepository.save(flag);
                savedFlag.setIsNew(false);
                savedFlags.add(savedFlag);
            }
            savedResult.setFlags(savedFlags);

            savedResults.add(savedResult);

            if (savedResult.getStatus() == ModerationStatus.FLAGGED) {
                log.warn("[SAVE] FLAGGED {} {} for pet={} with {} issues",
                        savedResult.getContentType(), savedResult.getContentIdentifier(),
                        savedResult.getPetId(), savedFlags.size());
                for (FlaggedContent flag : savedFlags) {
                    log.warn("[SAVE]   - [{}] {}: {}",
                            flag.getSeverity(), flag.getCategory(), flag.getDescription());
                }
            } else {
                log.debug("[SAVE] Saved {} {} for pet={} status={}",
                        savedResult.getContentType(), savedResult.getContentIdentifier(),
                        savedResult.getPetId(), savedResult.getStatus());
            }
        }

        return savedResults;
    }

    /**
     * Check if Forever Home API is available.
     */
    public boolean isApiAvailable() {
        boolean available = apiClient.isAvailable();
        log.info("[ORCHESTRATOR] API availability check: {}", available ? "AVAILABLE" : "UNAVAILABLE");
        return available;
    }

    /**
     * Get current configuration status.
     */
    public String getConfigStatus() {
        return String.format("textEnabled=%s, imageEnabled=%s, debug=%s, textModel=%s, visionModel=%s",
                config.textEnabled(), config.imageEnabled(), config.debug(),
                config.textModel(), config.visionModel());
    }
}
