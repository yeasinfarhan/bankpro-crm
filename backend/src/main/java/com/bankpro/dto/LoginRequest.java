package com.bankpro.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body of POST /api/auth/login. Bean Validation rejects bad input before our code runs. */
public record LoginRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Enter a valid email address")
        @Size(max = 190, message = "Email is too long")
        String email,

        @NotBlank(message = "Password is required")
        @Size(max = 100, message = "Password is too long")
        String password
) {
    /** Compact constructor: runs when JSON is read, before validation, so " A@B.com " is accepted. */
    public LoginRequest {
        email = email == null ? null : email.trim();
    }

    /** Password is deliberately left out so it can never end up in a log line. */
    @Override
    public String toString() {
        return "LoginRequest[email=" + email + "]";
    }
}
