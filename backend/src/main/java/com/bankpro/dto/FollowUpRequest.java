package com.bankpro.dto;

import com.bankpro.entity.ActivityType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

/**
 * Schedule a follow-up.
 *
 * @param scheduledAt date-time WITH offset, e.g. "2026-10-20T10:30:00+06:00" (Bangladesh Standard Time)
 */
public record FollowUpRequest(
        @NotNull(message = "Customer is required") Long customerId,
        Long leadId,
        @NotNull(message = "Activity type is required") ActivityType activityType,
        @NotNull(message = "Date and time are required") OffsetDateTime scheduledAt,
        @Size(max = 2000, message = "Notes are too long") String notes,
        Long assignedOfficerId
) {
}
