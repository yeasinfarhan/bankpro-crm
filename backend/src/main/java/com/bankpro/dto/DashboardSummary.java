package com.bankpro.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The 8 dashboard KPIs. Definitions (also in docs/ARCHITECTURE.md §5):
 * <ul>
 *   <li>totalCustomers — non-archived customers now (officer: assigned to them)</li>
 *   <li>newCustomersThisMonth — created in the current Bangladesh calendar month</li>
 *   <li>activeLeads — leads now in NEW/CONTACTED/QUALIFIED/PROPOSAL</li>
 *   <li>leadsConverted — sales created in the range, excluding declined applications (one per lead)</li>
 *   <li>totalSalesValue — COMPLETED simulated sales in the range (approved loans/cards count on approval)</li>
 *   <li>depositSalesValue — the DEPOSIT-product part of totalSalesValue (sales volume, not balances)</li>
 *   <li>targetAchievementPct — average achievement of the targets in scope overlapping the range; null if none</li>
 *   <li>overdueFollowUps — scheduled activities whose time has passed</li>
 * </ul>
 */
public record DashboardSummary(
        LocalDate from,
        LocalDate to,
        long totalCustomers,
        long newCustomersThisMonth,
        long activeLeads,
        long leadsConverted,
        BigDecimal totalSalesValue,
        BigDecimal depositSalesValue,
        BigDecimal targetAchievementPct,
        int targetsCounted,
        long overdueFollowUps
) {
}
