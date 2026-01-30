package com.example.foreverhome.service;

import com.example.foreverhome.domain.geo.Coordinates;
import com.example.foreverhome.dto.RescueSearchResponse;
import com.example.foreverhome.dto.RescueWithDistance;
import com.example.foreverhome.repository.RescueGeoRepository;
import com.example.foreverhome.service.geo.GeocodingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service for searching rescue organizations with geo-based filtering.
 */
@Service
public class RescueSearchService {

    private static final Logger log = LoggerFactory.getLogger(RescueSearchService.class);

    private static final double DEFAULT_RADIUS_KM = 50.0;
    private static final double MAX_RADIUS_KM = 200.0;
    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final RescueGeoRepository rescueGeoRepository;
    private final GeocodingService geocodingService;

    public RescueSearchService(RescueGeoRepository rescueGeoRepository,
                               GeocodingService geocodingService) {
        this.rescueGeoRepository = rescueGeoRepository;
        this.geocodingService = geocodingService;
    }

    /**
     * Searches for rescues near the given coordinates.
     *
     * @param lat      latitude of search center
     * @param lon      longitude of search center
     * @param radiusKm search radius in kilometers (default 50, max 200)
     * @param limit    maximum results (default 20, max 100)
     * @return search response with results and metadata
     */
    public RescueSearchResponse searchNearby(double lat, double lon, Double radiusKm, Integer limit) {
        double effectiveRadius = normalizeRadius(radiusKm);
        int effectiveLimit = normalizeLimit(limit);

        log.debug("Searching rescues near ({}, {}) within {}km", lat, lon, effectiveRadius);

        List<RescueWithDistance> results = rescueGeoRepository.findNearby(
                lat, lon, effectiveRadius, effectiveLimit);

        return new RescueSearchResponse(
                results,
                new RescueSearchResponse.SearchCenter(lat, lon),
                effectiveRadius,
                results.size()
        );
    }

    /**
     * Searches for rescues near the given postal code.
     *
     * @param postalCode postal/zip code to search near
     * @param country    optional country code (e.g., "IE", "US")
     * @param radiusKm   search radius in kilometers
     * @param limit      maximum results
     * @return search response, or empty if postal code cannot be geocoded
     */
    public Optional<RescueSearchResponse> searchByPostalCode(String postalCode, String country,
                                                             Double radiusKm, Integer limit) {
        log.debug("Geocoding postal code: {} ({})", postalCode, country);

        return geocodingService.geocodePostalCode(postalCode, country)
                .map(coords -> searchNearby(coords.latitude(), coords.longitude(), radiusKm, limit));
    }

    /**
     * Searches for rescues near the given coordinates or postal code.
     * Coordinates take precedence if both are provided.
     *
     * @param lat        optional latitude
     * @param lon        optional longitude
     * @param postalCode optional postal code (used if lat/lon not provided)
     * @param country    optional country code
     * @param radiusKm   search radius in kilometers
     * @param limit      maximum results
     * @return search response, or empty if location cannot be determined
     */
    public Optional<RescueSearchResponse> search(Double lat, Double lon, String postalCode,
                                                  String country, Double radiusKm, Integer limit) {
        // Coordinates take precedence
        if (lat != null && lon != null) {
            return Optional.of(searchNearby(lat, lon, radiusKm, limit));
        }

        // Fall back to postal code
        if (postalCode != null && !postalCode.isBlank()) {
            return searchByPostalCode(postalCode, country, radiusKm, limit);
        }

        return Optional.empty();
    }

    /**
     * Gets statistics about rescues without coordinates.
     */
    public int countRescuesWithoutCoordinates() {
        return rescueGeoRepository.countWithoutCoordinates();
    }

    private double normalizeRadius(Double radiusKm) {
        if (radiusKm == null || radiusKm <= 0) {
            return DEFAULT_RADIUS_KM;
        }
        return Math.min(radiusKm, MAX_RADIUS_KM);
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
