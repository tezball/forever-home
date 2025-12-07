package com.example.foreverhome.domain.profile;

import java.util.StringJoiner;

/**
 * Value object representing a physical address.
 * Street is optional for privacy reasons.
 */
public record Address(
        String street,
        String city,
        String state,
        String postalCode,
        String country
) {
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
