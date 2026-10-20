package com.bankpro.dto;

import java.time.Instant;

/**
 * One entry in a customer's chronological history (newest first).
 *
 * @param kind INTERACTION, FOLLOW_UP_SCHEDULED, LEAD_CREATED, LEAD_CLOSED or SALE
 */
public record TimelineItem(String kind, Instant at, String title, String detail, String actor, String link) {
}
