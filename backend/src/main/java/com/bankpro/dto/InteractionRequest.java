package com.bankpro.dto;

import com.bankpro.entity.InteractionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

/** Log something that already happened. {@code occurredAt} defaults to now and cannot be in the future. */
public record InteractionRequest(
        @NotNull(message = "Interaction type is required") InteractionType interactionType,
        @NotBlank(message = "Summary is required") @Size(max = 2000, message = "Summary is too long") String summary,
        OffsetDateTime occurredAt,
        Long leadId
) {
}
