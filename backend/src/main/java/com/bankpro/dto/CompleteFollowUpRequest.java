package com.bankpro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompleteFollowUpRequest(
        @NotBlank(message = "Outcome is required") @Size(max = 500, message = "Outcome is too long") String outcome,
        @Size(max = 2000, message = "Notes are too long") String notes
) {
}
