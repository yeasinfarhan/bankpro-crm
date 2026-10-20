package com.bankpro.service;

import com.bankpro.dto.UserResponse;
import com.bankpro.entity.Role;
import com.bankpro.entity.AppUser;
import com.bankpro.exception.AuthenticationFailedException;
import com.bankpro.exception.ForbiddenException;
import com.bankpro.mapper.UserMapper;
import com.bankpro.repository.AppUserRepository;
import com.bankpro.security.AuthenticatedUser;
import com.bankpro.security.DataScope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final AppUserRepository users;

    public UserService(AppUserRepository users) {
        this.users = users;
    }

    /** Current user's profile, re-read from the database so a deactivated account is rejected. */
    public UserResponse me(AuthenticatedUser caller) {
        AppUser user = users.findWithBranchById(caller.id())
                .filter(AppUser::isActive)
                .orElseThrow(() -> new AuthenticationFailedException("Your account is no longer active."));
        return UserMapper.toResponse(user);
    }

    /**
     * Team list, scoped by role: ADMIN sees everyone, a BRANCH_MANAGER only their own branch.
     * Relationship officers have no team view (also blocked by @PreAuthorize in the controller).
     */
    public List<UserResponse> listTeam(AuthenticatedUser caller) {
        DataScope scope = DataScope.of(caller);
        List<AppUser> result = switch (scope.level()) {
            case ALL -> users.findAllByOrderByFullNameAsc();
            case BRANCH -> users.findByBranchIdOrderByFullNameAsc(scope.branchId());
            case OWN -> throw new ForbiddenException("You do not have permission to view the team.");
        };
        return result.stream().map(UserMapper::toResponse).toList();
    }

    /**
     * Active staff who can own customers and leads (officers and managers), for assignment dropdowns and filters.
     * Non-admins always get their own branch; ADMIN may pick a branch or get everyone.
     */
    public List<UserResponse> listAssignable(AuthenticatedUser caller, Long branchId) {
        var roles = EnumSet.of(Role.RELATIONSHIP_OFFICER, Role.BRANCH_MANAGER);
        List<AppUser> result;
        if (caller.role() == Role.ADMIN) {
            result = branchId == null
                    ? users.findByActiveTrueAndRoleInOrderByFullNameAsc(roles)
                    : users.findByBranchIdAndActiveTrueAndRoleInOrderByFullNameAsc(branchId, roles);
        } else {
            result = users.findByBranchIdAndActiveTrueAndRoleInOrderByFullNameAsc(caller.branchId(), roles);
        }
        return result.stream().map(UserMapper::toResponse).toList();
    }
}
