package com.bankpro.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.ZoneId;
import java.util.List;

/**
 * Typed view of the {@code app.*} settings in application.yml.
 * One place to see every custom setting; {@code @Validated} stops the app from starting if one is invalid.
 *
 * @param businessZone       time zone for "today", "this month" and target periods (Asia/Dhaka)
 * @param corsAllowedOrigins exact frontend origins allowed to call the API
 * @param jwt                access-token settings
 * @param loginRateLimit     brute-force protection for POST /api/auth/login
 */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @NotNull ZoneId businessZone,
        @NotEmpty List<String> corsAllowedOrigins,
        @NotNull @Valid Jwt jwt,
        @NotNull @Valid LoginRateLimit loginRateLimit
) {

    /**
     * @param secret               Base64-encoded HMAC key, at least 32 bytes after decoding (env JWT_SECRET)
     * @param ttl                  how long an access token is valid
     * @param issuer               value of the "iss" claim
     * @param allowEphemeralSecret dev only: if no secret is configured, generate a random one at startup
     */
    public record Jwt(
            String secret,
            @NotNull Duration ttl,
            @NotBlank String issuer,
            boolean allowEphemeralSecret
    ) {
    }

    /**
     * @param maxFailures failed attempts allowed per email + IP within the window
     * @param window      sliding time window
     */
    public record LoginRateLimit(
            @Min(1) int maxFailures,
            @NotNull Duration window
    ) {
    }
}
