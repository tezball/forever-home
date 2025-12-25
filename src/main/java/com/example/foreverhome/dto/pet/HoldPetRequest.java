package com.example.foreverhome.dto.pet;

import jakarta.validation.constraints.NotBlank;

public record HoldPetRequest(
        @NotBlank(message = "Reason is required when putting on hold")
        String reason
) {}
