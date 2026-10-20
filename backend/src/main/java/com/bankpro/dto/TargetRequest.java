package com.bankpro.dto;

import com.bankpro.entity.PeriodType;
import com.bankpro.entity.TargetCategory;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * @param userId      officer the target is for; null = whole-branch target
 * @param periodStart any date inside the month/quarter (normalised to its first day)
 * @param targetValue BDT for DEPOSIT_SALES, a whole number for the count categories
 */
public record TargetRequest(
        Long userId,
        @NotNull(message = "Period type is required") PeriodType periodType,
        @NotNull(message = "Period is required") LocalDate periodStart,
        @NotNull(message = "Category is required") TargetCategory category,
        @NotNull(message = "Target value is required")
        @DecimalMin(value = "0.01", message = "Target must be greater than zero")
        @DecimalMax(value = "999999999999.99", message = "Target is too large")
        @Digits(integer = 12, fraction = 2, message = "Use at most 2 decimal places")
        BigDecimal targetValue
) {
}
