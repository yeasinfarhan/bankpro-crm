package com.bankpro.dto;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record RescheduleRequest(@NotNull(message = "New date and time are required") OffsetDateTime scheduledAt) {
}
