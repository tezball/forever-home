package com.example.foreverhome.moderation.client.dto;

import java.util.List;
import java.util.UUID;

/**
 * Pet profile data from Forever Home API.
 */
public record PetProfileDto(
        UUID id,
        String name,
        String species,
        String breed,
        int age,
        String ageUnit,
        String sex,
        String size,
        String microchipId,
        String description,
        String healthNotes,
        String status,
        UUID fosterId,
        UUID rescueOrgId,
        List<String> imageUrls
) {
    /**
     * Check if pet has any text content to moderate.
     */
    public boolean hasTextContent() {
        return (name != null && !name.isBlank()) ||
               (description != null && !description.isBlank()) ||
               (healthNotes != null && !healthNotes.isBlank());
    }

    /**
     * Check if pet has any images to moderate.
     */
    public boolean hasImages() {
        return imageUrls != null && !imageUrls.isEmpty();
    }
}
