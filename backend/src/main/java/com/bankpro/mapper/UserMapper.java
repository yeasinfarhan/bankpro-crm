package com.bankpro.mapper;

import com.bankpro.dto.BranchResponse;
import com.bankpro.dto.UserResponse;
import com.bankpro.entity.AppUser;
import com.bankpro.entity.Branch;

/**
 * Converts entities to DTOs. Hand-written on purpose: explicit, easy to debug, and it guarantees
 * sensitive fields such as {@code passwordHash} are never copied into API responses.
 */
public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(AppUser user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole(),
                user.getJobTitle(), toResponse(user.getBranch()), user.isActive(), user.getLastLoginAt());
    }

    public static BranchResponse toResponse(Branch branch) {
        if (branch == null) {
            return null;
        }
        return new BranchResponse(branch.getId(), branch.getCode(), branch.getName(), branch.getCity());
    }
}
