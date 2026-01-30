package com.example.foreverhome.service.geo;

import com.example.foreverhome.domain.geo.Coordinates;
import com.example.foreverhome.domain.profile.Address;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Optional;

/**
 * HTTP client for OpenStreetMap Nominatim geocoding API.
 * <p>
 * Note: Nominatim has a strict rate limit of 1 request per second.
 * This client should be wrapped with rate limiting.
 */
@Component
public class NominatimGeocodingClient {

    private static final Logger log = LoggerFactory.getLogger(NominatimGeocodingClient.class);

    private final RestClient restClient;
    private final String baseUrl;

    public NominatimGeocodingClient(
            RestClient.Builder restClientBuilder,
            @Value("${geocoding.nominatim.url:https://nominatim.openstreetmap.org}") String baseUrl,
            @Value("${spring.application.name:forever-home}") String appName) {
        this.baseUrl = baseUrl;
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader("User-Agent", appName + "/1.0")
                .build();
    }

    /**
     * Geocodes an address using Nominatim search API.
     */
    public Optional<Coordinates> geocode(Address address) {
        if (address == null) {
            return Optional.empty();
        }

        String query = buildQueryString(address);
        if (query.isBlank()) {
            return Optional.empty();
        }

        return search(query, address.country());
    }

    /**
     * Geocodes a postal code.
     */
    public Optional<Coordinates> geocodePostalCode(String postalCode, String country) {
        if (postalCode == null || postalCode.isBlank()) {
            return Optional.empty();
        }
        return search(postalCode, country);
    }

    private Optional<Coordinates> search(String query, String countryCode) {
        try {
            var uriBuilder = UriComponentsBuilder.fromPath("/search")
                    .queryParam("q", query)
                    .queryParam("format", "json")
                    .queryParam("limit", 1);

            if (countryCode != null && !countryCode.isBlank()) {
                uriBuilder.queryParam("countrycodes", normalizeCountryCode(countryCode));
            }

            String uri = uriBuilder.build().toUriString();
            log.debug("Geocoding request: {}", uri);

            List<NominatimResult> results = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(NominatimResultList.class);

            if (results != null && !results.isEmpty()) {
                NominatimResult result = results.getFirst();
                double lat = Double.parseDouble(result.lat);
                double lon = Double.parseDouble(result.lon);
                log.debug("Geocoding result: {} -> ({}, {})", query, lat, lon);
                return Optional.of(new Coordinates(lat, lon));
            }

            log.debug("No geocoding results for: {}", query);
            return Optional.empty();

        } catch (Exception e) {
            log.warn("Geocoding failed for query '{}': {}", query, e.getMessage());
            return Optional.empty();
        }
    }

    private String buildQueryString(Address address) {
        StringBuilder sb = new StringBuilder();

        if (address.street() != null && !address.street().isBlank()) {
            sb.append(address.street()).append(", ");
        }
        if (address.city() != null && !address.city().isBlank()) {
            sb.append(address.city()).append(", ");
        }
        if (address.state() != null && !address.state().isBlank()) {
            sb.append(address.state()).append(", ");
        }
        if (address.postalCode() != null && !address.postalCode().isBlank()) {
            sb.append(address.postalCode()).append(", ");
        }
        if (address.country() != null && !address.country().isBlank()) {
            sb.append(address.country());
        }

        return sb.toString().replaceAll(",\\s*$", "").trim();
    }

    private String normalizeCountryCode(String country) {
        if (country == null) return null;
        // Handle common country names -> ISO codes
        return switch (country.toLowerCase().trim()) {
            case "ireland", "ie" -> "ie";
            case "united states", "usa", "us" -> "us";
            case "united kingdom", "uk", "gb" -> "gb";
            case "germany", "de" -> "de";
            case "france", "fr" -> "fr";
            default -> country.length() == 2 ? country.toLowerCase() : null;
        };
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NominatimResult(
            @JsonProperty("lat") String lat,
            @JsonProperty("lon") String lon,
            @JsonProperty("display_name") String displayName
    ) {}

    // Type alias for deserialization
    private static class NominatimResultList extends java.util.ArrayList<NominatimResult> {}
}
