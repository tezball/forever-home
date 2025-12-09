package com.example.foreverhome.dto.pet;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SubmitForReviewRequest(
        @NotNull(message = "Rescue organization ID is required")
        UUID rescueOrgId
) {}
