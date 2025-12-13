package com.example.foreverhome.dto.pet;

import com.example.foreverhome.domain.pet.PetImage;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Function;

public record PetImageDto(
        UUID id,
        UUID petId,
        String url,
        boolean isPrimary,
        int displayOrder,
        Instant uploadedAt
) {
    /**
     * Creates a DTO from a PetImage entity, using the provided URL builder to construct the URL.
     *
     * @param image      the PetImage entity
     * @param urlBuilder function that converts an S3 key to a public URL
     * @return the populated DTO with a constructed URL
     */
    public static PetImageDto from(PetImage image, Function<String, String> urlBuilder) {
        return new PetImageDto(
                image.getId(),
                image.getPetId(),
                urlBuilder.apply(image.getS3Key()),
                image.isPrimary(),
                image.getDisplayOrder(),
                image.getUploadedAt()
        );
    }
}
