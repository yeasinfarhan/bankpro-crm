package com.bankpro.dto;

import com.bankpro.entity.ActivityType;
import com.bankpro.entity.FollowUpStatus;

import java.time.Instant;

/** @param overdue derived: still SCHEDULED and the time has passed */
public record FollowUpResponse(
        Long id,
        SaleResponse.Ref customer,
        SaleResponse.Ref lead,
        UserSummary assignedOfficer,
        ActivityType activityType,
        Instant scheduledAt,
        FollowUpStatus status,
        boolean overdue,
        String notes,
        String outcome,
        Instant completedAt,
        Instant createdAt,
        boolean canEdit
) {
    public record Counts(long today, long upcoming, long overdue) {
    }
}
