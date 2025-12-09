package com.example.foreverhome.dto.adoption;

import jakarta.validation.constraints.NotBlank;

public record RejectApplicationRequest(
        @NotBlank(message = "Reason is required when rejecting")
        String reason
) {}
