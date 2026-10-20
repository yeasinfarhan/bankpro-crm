package com.bankpro.dto;

import java.time.Instant;

/**
 * Response for GET /api/system/info.
 * DTOs (Data Transfer Objects) are the shapes we send over HTTP. Java records are ideal:
 * immutable, concise, and Jackson serialises them to JSON automatically.
 */
public record SystemInfoResponse(
        String application,
        String version,
        String businessZone,
        Instant serverTimeUtc,
        String businessLocalTime
) {
}
