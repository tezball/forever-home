package com.example.foreverhome.dto.pet;

import jakarta.validation.constraints.NotBlank;

public record DeclinePetRequest(
        @NotBlank(message = "Reason is required when declining")
        String reason
) {}
