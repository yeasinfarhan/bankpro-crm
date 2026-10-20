package com.bankpro.dto;

import com.bankpro.entity.LeadStatus;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Pipeline figures for the leads in view, computed by SQL aggregation.
 *
 * @param openCount          NEW + CONTACTED + QUALIFIED + PROPOSAL
 * @param openPipelineValue  sum of estimated values of open leads (an estimate, NOT sales)
 * @param conversionRate     converted ÷ (converted + lost) × 100, i.e. of leads that reached an outcome;
 *                           null when no lead has closed yet (avoids divide-by-zero)
 */
public record LeadSummaryResponse(
        Map<LeadStatus, Long> countByStatus,
        Map<LeadStatus, BigDecimal> valueByStatus,
        long openCount,
        BigDecimal openPipelineValue,
        long convertedCount,
        long lostCount,
        BigDecimal conversionRate
) {
}
