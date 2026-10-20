package com.bankpro.dto;

import com.bankpro.entity.ProductCategory;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * Create/update body for a banking product (ADMIN only).
 * {@code code} is only used on create; it cannot be changed later because leads and sales refer to products.
 */
public record ProductRequest(
        @Pattern(regexp = "^[A-Z0-9][A-Z0-9-]{1,29}$", message = "Use 2–30 capital letters, digits or hyphens")
        String code,

        @NotBlank(message = "Name is required") @Size(max = 120, message = "Name is too long")
        String name,

        @NotNull(message = "Category is required")
        ProductCategory category,

        @NotBlank(message = "Description is required") @Size(max = 2000, message = "Description is too long")
        String description,

        @Size(max = 2000, message = "Eligibility notes are too long")
        String eligibilityNotes,

        @Size(max = 8, message = "At most 8 benefits")
        List<@NotBlank(message = "Benefit cannot be empty") @Size(max = 120, message = "Benefit is too long") String> benefits,

        @DecimalMin(value = "0.00", message = "Rate cannot be negative")
        @DecimalMax(value = "100.00", message = "Rate cannot exceed 100%")
        @Digits(integer = 3, fraction = 2, message = "Use at most 2 decimal places")
        BigDecimal illustrativeRate,

        Boolean active
) {
}
