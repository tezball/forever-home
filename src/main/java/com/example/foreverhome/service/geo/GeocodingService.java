package com.example.foreverhome.service.geo;

import com.example.foreverhome.domain.geo.Coordinates;
import com.example.foreverhome.domain.profile.Address;

import java.util.Optional;

/**
 * Service for converting addresses to geographic coordinates.
 */
public interface GeocodingService {

    /**
     * Geocodes a full address to coordinates.
     *
     * @param address the address to geocode
     * @return coordinates if geocoding succeeded, empty otherwise
     */
    Optional<Coordinates> geocode(Address address);

    /**
     * Geocodes a postal/zip code to coordinates (center point).
     *
     * @param postalCode the postal code
     * @param country optional country code (e.g., "IE", "US")
     * @return coordinates if geocoding succeeded, empty otherwise
     */
    Optional<Coordinates> geocodePostalCode(String postalCode, String country);
}
