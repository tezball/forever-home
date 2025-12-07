package com.example.foreverhome.dto.pet;

import com.example.foreverhome.domain.pet.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PetDto(
        UUID id,
        String name,
        Species species,
        String breed,
        int age,
        AgeUnit ageUnit,
        PetSex sex,
        PetSize size,
        String microchipId,
        String description,
        String healthNotes,
        PetStatus status,
        UUID fosterId,
        UUID rescueOrgId,
        Instant createdAt,
        List<String> imageUrls
) {
    public static PetDto from(Pet pet) {
        return new PetDto(
                pet.getId(),
                pet.getName(),
                pet.getSpecies(),
                pet.getBreed(),
                pet.getAge(),
                pet.getAgeUnit(),
                pet.getSex(),
                pet.getSize(),
                pet.getMicrochipId(),
                pet.getDescription(),
                pet.getHealthNotes(),
                pet.getStatus(),
                pet.getFosterId(),
                pet.getRescueOrgId(),
                pet.getCreatedAt(),
                List.of() // Images loaded separately
        );
    }

    public static PetDto from(Pet pet, List<String> imageUrls) {
        return new PetDto(
                pet.getId(),
                pet.getName(),
                pet.getSpecies(),
                pet.getBreed(),
                pet.getAge(),
                pet.getAgeUnit(),
                pet.getSex(),
                pet.getSize(),
                pet.getMicrochipId(),
                pet.getDescription(),
                pet.getHealthNotes(),
                pet.getStatus(),
                pet.getFosterId(),
                pet.getRescueOrgId(),
                pet.getCreatedAt(),
                imageUrls
        );
    }
}
