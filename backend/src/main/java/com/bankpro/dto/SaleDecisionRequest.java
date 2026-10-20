package com.bankpro.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** A manager's SIMULATED decision on a pending loan/card application. Not a real credit decision. */
public record SaleDecisionRequest(
        @NotNull(message = "Decision is required") Decision decision,
        @Size(max = 500, message = "Note is too long") String note
) {
    public enum Decision { APPROVE, DECLINE }
}
