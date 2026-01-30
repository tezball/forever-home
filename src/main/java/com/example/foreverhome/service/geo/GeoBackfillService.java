package com.example.foreverhome.service.geo;

import com.example.foreverhome.domain.geo.Coordinates;
import com.example.foreverhome.domain.profile.RescueOrganization;
import com.example.foreverhome.repository.RescueGeoRepository;
import com.example.foreverhome.repository.RescueOrganizationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for backfilling geographic coordinates for existing rescue organizations.
 */
@Service
public class GeoBackfillService {

    private static final Logger log = LoggerFactory.getLogger(GeoBackfillService.class);

    private final RescueOrganizationRepository rescueRepository;
    private final RescueGeoRepository rescueGeoRepository;
    private final GeocodingService geocodingService;

    public GeoBackfillService(RescueOrganizationRepository rescueRepository,
                              RescueGeoRepository rescueGeoRepository,
                              GeocodingService geocodingService) {
        this.rescueRepository = rescueRepository;
        this.rescueGeoRepository = rescueGeoRepository;
        this.geocodingService = geocodingService;
    }

    /**
     * Returns the number of rescue organizations without coordinates.
     */
    public int countRescuesWithoutCoordinates() {
        return rescueGeoRepository.countWithoutCoordinates();
    }

    /**
     * Backfills coordinates for all rescue organizations that don't have them.
     * This is an async operation that runs in the background.
     *
     * @param batchSize maximum number of rescues to process in one run
     * @return CompletableFuture with the result statistics
     */
    @Async
    public CompletableFuture<BackfillResult> backfillCoordinates(int batchSize) {
        log.info("Starting geocode backfill for up to {} rescues", batchSize);

        List<UUID> rescueIds = rescueGeoRepository.findIdsWithoutCoordinates(batchSize);

        if (rescueIds.isEmpty()) {
            log.info("No rescues need geocoding");
            return CompletableFuture.completedFuture(new BackfillResult(0, 0, 0));
        }

        AtomicInteger processed = new AtomicInteger(0);
        AtomicInteger succeeded = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);

        for (UUID rescueId : rescueIds) {
            try {
                processRescue(rescueId, succeeded, failed);
            } catch (Exception e) {
                log.error("Error processing rescue {}: {}", rescueId, e.getMessage());
                failed.incrementAndGet();
            }
            processed.incrementAndGet();

            // Log progress every 10 rescues
            if (processed.get() % 10 == 0) {
                log.info("Backfill progress: {}/{} processed, {} succeeded, {} failed",
                        processed.get(), rescueIds.size(), succeeded.get(), failed.get());
            }
        }

        BackfillResult result = new BackfillResult(processed.get(), succeeded.get(), failed.get());
        log.info("Geocode backfill complete: {}", result);
        return CompletableFuture.completedFuture(result);
    }

    private void processRescue(UUID rescueId, AtomicInteger succeeded, AtomicInteger failed) {
        rescueRepository.findById(rescueId).ifPresent(rescue -> {
            if (rescue.getAddress() == null) {
                log.debug("Skipping rescue {} - no address", rescueId);
                failed.incrementAndGet();
                return;
            }

            geocodingService.geocode(rescue.getAddress())
                    .ifPresentOrElse(
                            coords -> {
                                rescueGeoRepository.updateCoordinates(rescueId, coords.latitude(), coords.longitude());
                                log.debug("Updated coordinates for rescue {}: ({}, {})",
                                        rescueId, coords.latitude(), coords.longitude());
                                succeeded.incrementAndGet();
                            },
                            () -> {
                                log.debug("Failed to geocode rescue {}", rescueId);
                                failed.incrementAndGet();
                            }
                    );
        });
    }

    /**
     * Geocodes a single rescue organization and updates its coordinates.
     *
     * @param rescueId the rescue organization ID
     * @return the coordinates if geocoding succeeded, empty otherwise
     */
    public java.util.Optional<Coordinates> geocodeRescue(UUID rescueId) {
        return rescueRepository.findById(rescueId)
                .flatMap(rescue -> {
                    if (rescue.getAddress() == null) {
                        return java.util.Optional.empty();
                    }
                    return geocodingService.geocode(rescue.getAddress())
                            .map(coords -> {
                                rescueGeoRepository.updateCoordinates(rescueId, coords.latitude(), coords.longitude());
                                return coords;
                            });
                });
    }

    /**
     * Result of a backfill operation.
     */
    public record BackfillResult(int processed, int succeeded, int failed) {
        @Override
        public String toString() {
            return String.format("processed=%d, succeeded=%d, failed=%d", processed, succeeded, failed);
        }
    }
}
