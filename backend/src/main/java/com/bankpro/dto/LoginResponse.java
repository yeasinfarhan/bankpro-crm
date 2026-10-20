package com.bankpro.dto;

import java.time.Instant;

/**
 * Returned after a successful login.
 *
 * @param expiresIn seconds until the token expires (convenient for the frontend)
 */
public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        Instant expiresAt,
        UserResponse user
) {
}
