package com.example.foreverhome.dto.pet;

import com.example.foreverhome.domain.pet.PetStatusHistory;

import java.time.Instant;
import java.util.UUID;

public record PetStatusHistoryDto(
        UUID id,
        UUID petId,
        String fromStatus,
        String toStatus,
        UUID changedBy,
        Instant changedAt,
        String notes
) {
    public static PetStatusHistoryDto from(PetStatusHistory history) {
        return new PetStatusHistoryDto(
                history.getId(),
                history.getPetId(),
                history.getFromStatus() != null ? history.getFromStatus().name() : null,
                history.getToStatus().name(),
                history.getChangedBy(),
                history.getChangedAt(),
                history.getNotes()
        );
    }
}
