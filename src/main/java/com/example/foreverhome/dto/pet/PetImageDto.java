package com.example.foreverhome.dto.pet;

import com.example.foreverhome.domain.pet.PetImage;

import java.time.Instant;
import java.util.UUID;

public record PetImageDto(
        UUID id,
        UUID petId,
        String url,
        boolean isPrimary,
        int displayOrder,
        Instant uploadedAt
) {
    public static PetImageDto from(PetImage image) {
        return new PetImageDto(
                image.getId(),
                image.getPetId(),
                image.getUrl(),
                image.isPrimary(),
                image.getDisplayOrder(),
                image.getUploadedAt()
        );
    }
}
