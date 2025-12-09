package com.example.foreverhome.dto.adoption;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record FinalizeAdoptionRequest(
        @NotNull(message = "Application ID is required")
        UUID applicationId
) {}
