package com.bankpro.dto;

import com.bankpro.entity.CustomerSegment;
import com.bankpro.entity.CustomerStatus;
import com.bankpro.entity.EmploymentType;
import com.bankpro.entity.IncomeRange;

import java.time.Instant;
import java.util.List;

/**
 * @param code        display code derived from the id (e.g. CUS-000042), not stored (D-14)
 * @param products    only filled on the detail endpoint
 * @param permissions what the CALLER may do with this record, so the UI can hide buttons;
 *                    the backend enforces the same rules regardless
 */
public record CustomerResponse(
        Long id,
        String code,
        String fullName,
        String email,
        String phone,
        String occupation,
        EmploymentType employmentType,
        IncomeRange incomeRange,
        String city,
        CustomerSegment segment,
        CustomerStatus status,
        BranchResponse branch,
        UserSummary assignedOfficer,
        Instant lastInteractionAt,
        Instant createdAt,
        Instant updatedAt,
        long version,
        List<HeldProduct> products,
        Permissions permissions
) {
    public record HeldProduct(Long productId, String code, String name, String category, String since, boolean fromSale) {
    }

    public record Permissions(boolean canEdit, boolean canArchive) {
    }
}
