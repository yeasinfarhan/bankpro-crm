package com.bankpro.dto;

import com.bankpro.entity.LeadPriority;
import com.bankpro.entity.LeadSource;
import com.bankpro.entity.LeadStatus;
import com.bankpro.entity.LossReason;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

/**
 * @param allowedStatuses stage moves the CALLER may make next (excludes CONVERTED, which uses the convert action)
 * @param sale            only filled on the detail endpoint, when the lead has been converted
 */
public record LeadResponse(
        Long id,
        String code,
        SaleResponse.Ref customer,
        SaleResponse.Ref product,
        String productCategory,
        BranchResponse branch,
        UserSummary assignedOfficer,
        LeadSource source,
        LeadStatus status,
        LeadPriority priority,
        BigDecimal estimatedValue,
        LocalDate expectedCloseDate,
        LossReason lossReason,
        String lossNote,
        String notes,
        Instant lastContactedAt,
        Instant statusChangedAt,
        Instant createdAt,
        Instant updatedAt,
        long version,
        Set<LeadStatus> allowedStatuses,
        Permissions permissions,
        SaleResponse sale
) {
    public record Permissions(boolean canEdit, boolean canAssign, boolean canConvert) {
    }
}
