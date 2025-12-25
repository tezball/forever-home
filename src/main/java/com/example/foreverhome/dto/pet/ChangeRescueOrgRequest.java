package com.example.foreverhome.dto.pet;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ChangeRescueOrgRequest(
        @NotNull(message = "New rescue organization ID is required")
        UUID rescueOrgId
) {}
