package com.bankpro.entity;

/** Lifecycle of a scheduled activity. Overdue is derived (SCHEDULED and in the past), not stored. */
public enum FollowUpStatus {
    SCHEDULED, COMPLETED, CANCELLED
}
