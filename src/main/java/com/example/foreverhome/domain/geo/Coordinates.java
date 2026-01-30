package com.example.foreverhome.domain.geo;

/**
 * Value object representing geographic coordinates.
 */
public record Coordinates(double latitude, double longitude) {

    /**
     * Validates that coordinates are within valid ranges.
     */
    public Coordinates {
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90");
        }
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180");
        }
    }

    /**
     * Calculates the distance to another coordinate using the Haversine formula.
     *
     * @param other the other coordinate
     * @return distance in kilometers
     */
    public double distanceTo(Coordinates other) {
        final double R = 6371; // Earth's radius in kilometers

        double lat1Rad = Math.toRadians(this.latitude);
        double lat2Rad = Math.toRadians(other.latitude);
        double deltaLat = Math.toRadians(other.latitude - this.latitude);
        double deltaLon = Math.toRadians(other.longitude - this.longitude);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }
}
