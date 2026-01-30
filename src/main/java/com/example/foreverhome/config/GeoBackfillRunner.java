package com.example.foreverhome.config;

import com.example.foreverhome.service.geo.GeoBackfillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Runner that triggers geocode backfill on application startup if enabled.
 *
 * Enable with environment variable: GEO_BACKFILL_ENABLED=true
 * Configure batch size with: GEO_BACKFILL_BATCH_SIZE=100
 */
@Component
public class GeoBackfillRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(GeoBackfillRunner.class);

    private final GeoBackfillService backfillService;
    private final boolean enabled;
    private final int batchSize;

    public GeoBackfillRunner(
            GeoBackfillService backfillService,
            @Value("${geo.backfill.enabled:false}") boolean enabled,
            @Value("${geo.backfill.batch-size:100}") int batchSize) {
        this.backfillService = backfillService;
        this.enabled = enabled;
        this.batchSize = batchSize;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.debug("Geocode backfill is disabled");
            return;
        }

        int pending = backfillService.countRescuesWithoutCoordinates();
        if (pending == 0) {
            log.info("No rescues need geocoding");
            return;
        }

        log.info("Starting geocode backfill for {} rescues (batch size: {})", pending, batchSize);

        backfillService.backfillCoordinates(batchSize)
                .thenAccept(result -> {
                    log.info("Geocode backfill completed: {}", result);

                    int remaining = backfillService.countRescuesWithoutCoordinates();
                    if (remaining > 0) {
                        log.info("{} rescues still need geocoding. Run another backfill or check addresses.", remaining);
                    }
                })
                .exceptionally(e -> {
                    log.error("Geocode backfill failed", e);
                    return null;
                });
    }
}
