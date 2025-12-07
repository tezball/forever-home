package com.example.foreverhome.dto.pet;

import com.example.foreverhome.domain.pet.AgeUnit;
import com.example.foreverhome.domain.pet.PetSex;
import com.example.foreverhome.domain.pet.PetSize;
import com.example.foreverhome.domain.pet.Species;

public record UpdatePetRequest(
        String name,
        Species species,
        String breed,
        Integer age,
        AgeUnit ageUnit,
        PetSex sex,
        PetSize size,
        String microchipId,
        String description,
        String healthNotes
) {
}
