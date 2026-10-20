package com.bankpro.dto;

/** Minimal user reference embedded in other responses (e.g. a customer's assigned officer). */
public record UserSummary(Long id, String fullName, String jobTitle) {
}
