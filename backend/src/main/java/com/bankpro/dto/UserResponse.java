package com.bankpro.dto;

import com.bankpro.entity.Role;

import java.time.Instant;

/** Public view of an employee. Never includes the password hash. */
public record UserResponse(
        Long id,
        String email,
        String fullName,
        Role role,
        String jobTitle,
        BranchResponse branch,
        boolean active,
        Instant lastLoginAt
) {
}
