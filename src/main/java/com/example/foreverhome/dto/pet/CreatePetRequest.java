package com.example.foreverhome.dto.pet;

import com.example.foreverhome.domain.pet.AgeUnit;
import com.example.foreverhome.domain.pet.Breed;
import com.example.foreverhome.domain.pet.PetSex;
import com.example.foreverhome.domain.pet.PetSize;
import com.example.foreverhome.domain.pet.Species;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreatePetRequest(
        @NotBlank(message = "Name is required")
        String name,

        @NotNull(message = "Species is required")
        Species species,

        Breed breed,

        @Min(value = 0, message = "Age must be non-negative")
        int age,

        @NotNull(message = "Age unit is required")
        AgeUnit ageUnit,

        @NotNull(message = "Sex is required")
        PetSex sex,

        @NotNull(message = "Size is required")
        PetSize size,

        @NotBlank(message = "Microchip ID is required")
        String microchipId,

        String description,

        String healthNotes
) {
}
