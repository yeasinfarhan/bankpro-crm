package com.bankpro.dto;

import jakarta.validation.constraints.NotNull;

public record AssignRequest(@NotNull(message = "Officer is required") Long officerId) {
}
