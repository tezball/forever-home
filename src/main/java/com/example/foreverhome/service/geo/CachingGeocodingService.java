package com.example.foreverhome.service.geo;

import com.example.foreverhome.domain.geo.Coordinates;
import com.example.foreverhome.domain.geo.GeocodeCache;
import com.example.foreverhome.domain.profile.Address;
import com.example.foreverhome.repository.GeocodeCacheRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;

/**
 * Geocoding service with caching and rate limiting.
 * <p>
 * Wraps the Nominatim client with:
 * - Persistent cache to reduce API calls
 * - Rate limiting (1 req/sec for Nominatim compliance)
 * - Automatic cache cleanup
 */
@Service
public class CachingGeocodingService implements GeocodingService {

    private static final Logger log = LoggerFactory.getLogger(CachingGeocodingService.class);

    private final NominatimGeocodingClient client;
    private final GeocodeCacheRepository cacheRepository;
    private final SimpleRateLimiter rateLimiter;
    private final boolean enabled;

    public CachingGeocodingService(
            NominatimGeocodingClient client,
            GeocodeCacheRepository cacheRepository,
            @Value("${geocoding.rate-limit:1.0}") double rateLimit,
            @Value("${geocoding.enabled:true}") boolean enabled) {
        this.client = client;
        this.cacheRepository = cacheRepository;
        this.rateLimiter = SimpleRateLimiter.create(rateLimit);
        this.enabled = enabled;
    }

    @Override
    public Optional<Coordinates> geocode(Address address) {
        if (!enabled || address == null) {
            return Optional.empty();
        }

        String cacheKey = buildCacheKey(address);
        return getFromCacheOrFetch(cacheKey, () -> client.geocode(address));
    }

    @Override
    public Optional<Coordinates> geocodePostalCode(String postalCode, String country) {
        if (!enabled || postalCode == null || postalCode.isBlank()) {
            return Optional.empty();
        }

        String cacheKey = buildPostalCodeCacheKey(postalCode, country);
        return getFromCacheOrFetch(cacheKey, () -> client.geocodePostalCode(postalCode, country));
    }

    private Optional<Coordinates> getFromCacheOrFetch(String cacheKey, java.util.function.Supplier<Optional<Coordinates>> fetcher) {
        // Check cache first
        Optional<GeocodeCache> cached = cacheRepository.findByCacheKey(cacheKey);
        if (cached.isPresent() && !cached.get().isExpired()) {
            log.debug("Cache hit for key: {}", cacheKey);
            return Optional.of(cached.get().toCoordinates());
        }

        // Rate limit before making external call
        rateLimiter.acquire();

        // Fetch from external service
        Optional<Coordinates> result = fetcher.get();

        // Cache the result if successful
        result.ifPresent(coords -> {
            GeocodeCache cache = new GeocodeCache(cacheKey, coords.latitude(), coords.longitude());
            cacheRepository.save(cache);
            log.debug("Cached geocoding result for key: {}", cacheKey);
        });

        return result;
    }

    /**
     * Builds a normalized cache key from an address.
     */
    private String buildCacheKey(Address address) {
        StringBuilder sb = new StringBuilder("addr:");

        if (address.street() != null && !address.street().isBlank()) {
            sb.append(normalize(address.street())).append("|");
        }
        if (address.city() != null && !address.city().isBlank()) {
            sb.append(normalize(address.city())).append("|");
        }
        if (address.state() != null && !address.state().isBlank()) {
            sb.append(normalize(address.state())).append("|");
        }
        if (address.postalCode() != null && !address.postalCode().isBlank()) {
            sb.append(normalize(address.postalCode())).append("|");
        }
        if (address.country() != null && !address.country().isBlank()) {
            sb.append(normalize(address.country()));
        }

        return sb.toString().replaceAll("\\|+$", "");
    }

    private String buildPostalCodeCacheKey(String postalCode, String country) {
        String key = "postal:" + normalize(postalCode);
        if (country != null && !country.isBlank()) {
            key += "|" + normalize(country);
        }
        return key;
    }

    private String normalize(String s) {
        return s.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }

    /**
     * Cleans up expired cache entries daily.
     */
    @Scheduled(cron = "0 0 3 * * *") // 3 AM daily
    public void cleanupExpiredCache() {
        int deleted = cacheRepository.deleteExpiredEntries();
        if (deleted > 0) {
            log.info("Cleaned up {} expired geocode cache entries", deleted);
        }
    }
}
