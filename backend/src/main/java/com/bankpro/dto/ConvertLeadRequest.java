package com.bankpro.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

/**
 * POST /api/leads/{id}/convert.
 *
 * @param amount the simulated sale amount in BDT (deposit/opening amount, loan or card limit applied for)
 */
public record ConvertLeadRequest(
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        @DecimalMax(value = "999999999999.99", message = "Amount is too large")
        @Digits(integer = 12, fraction = 2, message = "Use at most 2 decimal places")
        java.math.BigDecimal amount
) {
}
