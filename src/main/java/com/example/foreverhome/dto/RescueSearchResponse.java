package com.example.foreverhome.dto;

import java.util.List;

/**
 * Response DTO for geo-based rescue search results.
 */
public record RescueSearchResponse(
        List<RescueWithDistance> results,
        SearchCenter searchCenter,
        double radiusKm,
        int totalResults
) {
    /**
     * The center point of the search.
     * Note: Coordinates are included for display purposes but should not expose
     * exact rescue locations in the results.
     */
    public record SearchCenter(double latitude, double longitude) {}
}
