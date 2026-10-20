package com.bankpro.dto;

import com.bankpro.entity.PeriodType;
import com.bankpro.entity.TargetCategory;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * @param actual         computed from records at request time
 * @param achievementPct actual ÷ target × 100 (1 decimal); may exceed 100
 * @param remaining      max(target − actual, 0)
 * @param unit           BDT or COUNT
 */
public record TargetResponse(
        Long id,
        BranchResponse branch,
        UserSummary user,
        PeriodType periodType,
        LocalDate periodStart,
        LocalDate periodEnd,
        TargetCategory category,
        String unit,
        BigDecimal targetValue,
        BigDecimal actual,
        BigDecimal achievementPct,
        BigDecimal remaining,
        boolean canEdit
) {
    public record Ranking(int rank, UserSummary officer, int targets, BigDecimal averageAchievementPct,
                          int targetsMet) {
    }
}
