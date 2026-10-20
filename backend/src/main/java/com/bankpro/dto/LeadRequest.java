package com.bankpro.dto;

import com.bankpro.entity.LeadPriority;
import com.bankpro.entity.LeadSource;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Create/update body for a lead. Status is NOT part of this request — stage changes use PATCH /status
 * so they always pass through the pipeline rules.
 *
 * @param customerId        used on create only (a lead cannot move to another customer)
 * @param assignedOfficerId managers may choose an officer in their branch; officers always own their leads
 * @param estimatedValue    expected opportunity value in BDT — NOT a completed sale
 */
public record LeadRequest(
        Long customerId,
        @NotNull(message = "Product is required") Long productId,
        @NotNull(message = "Lead source is required") LeadSource source,
        LeadPriority priority,
        @NotNull(message = "Estimated value is required")
        @DecimalMin(value = "0.00", message = "Estimated value cannot be negative")
        @DecimalMax(value = "999999999999.99", message = "Estimated value is too large")
        @Digits(integer = 12, fraction = 2, message = "Use at most 2 decimal places")
        BigDecimal estimatedValue,
        LocalDate expectedCloseDate,
        @Size(max = 2000, message = "Notes are too long") String notes,
        Long assignedOfficerId,
        Long version
) {
}
