package com.example.foreverhome.moderation.client;

import com.example.foreverhome.moderation.client.dto.PagedResponse;
import com.example.foreverhome.moderation.client.dto.PetProfileDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Client for Forever Home REST API.
 */
@Component
public class ForeverHomeApiClient {

    private static final Logger log = LoggerFactory.getLogger(ForeverHomeApiClient.class);

    private final RestClient restClient;
    private final String baseUrl;
    private final String imageUrlRewriteFrom;
    private final String imageUrlRewriteTo;

    public ForeverHomeApiClient(
            @Value("${foreverhome.api.base-url}") String baseUrl,
            @Value("${foreverhome.api.token:}") String apiToken,
            @Value("${foreverhome.api.image-url-rewrite-from:}") String imageUrlRewriteFrom,
            @Value("${foreverhome.api.image-url-rewrite-to:}") String imageUrlRewriteTo) {
        this.baseUrl = baseUrl;
        this.imageUrlRewriteFrom = imageUrlRewriteFrom;
        this.imageUrlRewriteTo = imageUrlRewriteTo;

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(baseUrl);

        if (apiToken != null && !apiToken.isBlank()) {
            builder.defaultHeader("Authorization", "Bearer " + apiToken);
        }

        this.restClient = builder.build();
        log.info("Initialized ForeverHomeApiClient with base URL: {}", baseUrl);
        if (imageUrlRewriteFrom != null && !imageUrlRewriteFrom.isBlank()) {
            log.info("Image URL rewrite enabled: {} -> {}", imageUrlRewriteFrom, imageUrlRewriteTo);
        }
    }

    /**
     * Fetch a single pet by ID.
     */
    public Optional<PetProfileDto> getPet(UUID petId) {
        try {
            PetProfileDto pet = restClient.get()
                    .uri("/api/pets/{id}", petId)
                    .retrieve()
                    .body(PetProfileDto.class);
            return Optional.ofNullable(pet);
        } catch (RestClientException e) {
            log.error("Failed to fetch pet {}: {}", petId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Fetch available pets with pagination.
     */
    public PagedResponse<PetProfileDto> getAvailablePets(int page, int pageSize) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/pets")
                            .queryParam("page", page)
                            .queryParam("pageSize", pageSize)
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (RestClientException e) {
            log.error("Failed to fetch available pets: {}", e.getMessage());
            return new PagedResponse<>(List.of(), page, pageSize, 0, 0, true, true);
        }
    }

    /**
     * Fetch all available pets up to a limit.
     * @deprecated Use {@link #getPetsPendingModeration(int)} for moderation batch processing.
     */
    @Deprecated
    public List<PetProfileDto> getAllAvailablePets(int limit) {
        List<PetProfileDto> allPets = new ArrayList<>();
        int page = 0;
        int pageSize = Math.min(limit, 50);

        while (allPets.size() < limit) {
            PagedResponse<PetProfileDto> response = getAvailablePets(page, pageSize);
            if (response.content().isEmpty()) {
                break;
            }

            for (PetProfileDto pet : response.content()) {
                if (allPets.size() >= limit) {
                    break;
                }
                allPets.add(pet);
            }

            if (!response.hasMore()) {
                break;
            }
            page++;
        }

        return allPets;
    }

    /**
     * Fetch pets that are pending moderation (moderation_status = 'PENDING').
     * This is the correct endpoint for batch moderation processing.
     */
    public List<PetProfileDto> getPetsPendingModeration(int limit) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/dev/moderation/pending")
                            .queryParam("limit", limit)
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (RestClientException e) {
            log.error("Failed to fetch pets pending moderation: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Fetch image bytes from a URL.
     */
    public byte[] fetchImageBytes(String imageUrl) {
        try {
            // If it's a relative URL, prefix with base URL
            String fullUrl = imageUrl.startsWith("http") ? imageUrl : baseUrl + imageUrl;

            // Apply URL rewrite for Docker networking (e.g., localhost:4566 -> localstack:4566)
            fullUrl = rewriteImageUrl(fullUrl);

            return RestClient.create()
                    .get()
                    .uri(fullUrl)
                    .retrieve()
                    .body(byte[].class);
        } catch (RestClientException e) {
            log.error("Failed to fetch image from {}: {}", imageUrl, e.getMessage());
            throw new RuntimeException("Failed to fetch image: " + e.getMessage(), e);
        }
    }

    /**
     * Rewrite image URL for Docker networking.
     * Translates localhost URLs to Docker service names when configured.
     */
    private String rewriteImageUrl(String url) {
        if (imageUrlRewriteFrom != null && !imageUrlRewriteFrom.isBlank()
                && imageUrlRewriteTo != null && url.startsWith(imageUrlRewriteFrom)) {
            String rewritten = url.replace(imageUrlRewriteFrom, imageUrlRewriteTo);
            log.debug("Rewrote image URL: {} -> {}", url, rewritten);
            return rewritten;
        }
        return url;
    }

    /**
     * Check if the API is available.
     */
    public boolean isAvailable() {
        try {
            restClient.get()
                    .uri("/actuator/health")
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientException e) {
            log.warn("Forever Home API not available: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Reset all pet moderation statuses to PENDING.
     * @return the number of pets affected, or -1 if the call failed
     */
    public int resetModerationStatuses() {
        try {
            var response = restClient.post()
                    .uri("/api/dev/moderation/reset")
                    .retrieve()
                    .body(new ParameterizedTypeReference<java.util.Map<String, Object>>() {});
            if (response != null && response.containsKey("petsAffected")) {
                int count = ((Number) response.get("petsAffected")).intValue();
                log.info("Reset moderation statuses for {} pets", count);
                return count;
            }
            return 0;
        } catch (RestClientException e) {
            log.error("Failed to reset moderation statuses: {}", e.getMessage());
            return -1;
        }
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}
