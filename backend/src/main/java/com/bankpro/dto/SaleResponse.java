package com.bankpro.dto;

import com.bankpro.entity.SaleStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record SaleResponse(
        Long id,
        Long leadId,
        String leadCode,
        Ref customer,
        Ref product,
        String productCategory,
        UserSummary officer,
        BigDecimal amount,
        SaleStatus status,
        Instant convertedAt,
        Instant decidedAt,
        String decisionNote,
        boolean canDecide
) {
    public record Ref(Long id, String code, String name) {
    }
}
