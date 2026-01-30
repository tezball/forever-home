package com.example.foreverhome.dto;

import java.util.UUID;

/**
 * Projection of RescueOrganization with calculated distance.
 * Used for geo-search results.
 */
public record RescueWithDistance(
        UUID id,
        String name,
        String description,
        String logoUrl,
        String city,
        String state,
        boolean verified,
        double distanceKm
) {
    /**
     * Formats distance for display.
     */
    public String formattedDistance() {
        if (distanceKm < 1) {
            return String.format("%.0f m", distanceKm * 1000);
        }
        return String.format("%.1f km", distanceKm);
    }
}
