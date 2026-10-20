package com.bankpro.dto;

import com.bankpro.entity.LeadStatus;
import com.bankpro.entity.LossReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** PATCH /api/leads/{id}/status. A loss reason is required when moving to LOST. */
public record LeadStatusRequest(
        @NotNull(message = "Status is required") LeadStatus status,
        LossReason lossReason,
        @Size(max = 500, message = "Note is too long") String lossNote
) {
}
