package com.bankpro.dto;

import com.bankpro.entity.ProductCategory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * @param illustrativeRate fictional sample value; the UI always labels it as such
 */
public record ProductResponse(
        Long id,
        String code,
        String name,
        ProductCategory category,
        String description,
        String eligibilityNotes,
        List<String> benefits,
        BigDecimal illustrativeRate,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
