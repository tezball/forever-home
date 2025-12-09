package com.example.foreverhome.dto.adoption;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SubmitApplicationRequest(
        @NotNull(message = "Pet ID is required")
        UUID petId,

        @Size(max = 1000, message = "Message cannot exceed 1000 characters")
        String message
) {}
