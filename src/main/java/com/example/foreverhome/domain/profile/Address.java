package com.example.foreverhome.domain.profile;

import java.util.StringJoiner;

/**
 * Value object representing a physical address with optional geolocation.
 * Street is optional for privacy reasons.
 * Latitude and longitude are populated via geocoding service.
 */
public record Address(
        String street,
        String city,
        String state,
        String postalCode,
        String country,
        Double latitude,
        Double longitude
) {
    /**
     * Creates an Address without coordinates (for backward compatibility).
     */
    public Address(String street, String city, String state, String postalCode, String country) {
        this(street, city, state, postalCode, country, null, null);
    }

    /**
     * Returns a new Address with the given coordinates.
     */
    public Address withCoordinates(Double latitude, Double longitude) {
        return new Address(street, city, state, postalCode, country, latitude, longitude);
    }

    /**
     * Checks if this address has valid coordinates.
     */
    public boolean hasCoordinates() {
        return latitude != null && longitude != null;
    }
    /**
     * Formats the address as a single line string.
     * Omits null components.
     * @return formatted address string
     */
    public String toSingleLine() {
        StringJoiner joiner = new StringJoiner(", ");

        if (street != null && !street.isBlank()) {
            joiner.add(street);
        }
        if (city != null && !city.isBlank()) {
            joiner.add(city);
        }

        // Combine state and postal code
        StringBuilder statePostal = new StringBuilder();
        if (state != null && !state.isBlank()) {
            statePostal.append(state);
        }
        if (postalCode != null && !postalCode.isBlank()) {
            if (!statePostal.isEmpty()) {
                statePostal.append(" ");
            }
            statePostal.append(postalCode);
        }
        if (!statePostal.isEmpty()) {
            joiner.add(statePostal.toString());
        }

        if (country != null && !country.isBlank()) {
            joiner.add(country);
        }

        return joiner.toString();
    }

    /**
     * Gets a short display format of city and state.
     * @return city and state formatted as "City, ST"
     */
    public String getCityState() {
        if (city == null && state == null) {
            return "";
        }
        if (city == null) {
            return state;
        }
        if (state == null) {
            return city;
        }
        return city + ", " + state;
    }
}
