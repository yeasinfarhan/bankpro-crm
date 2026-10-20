package com.bankpro.security;

import com.bankpro.entity.Role;

/**
 * Which records a caller may see, derived ONLY from the authenticated user:
 * <ul>
 *   <li>ALL    — ADMIN: every branch (read-only for branch business data)</li>
 *   <li>BRANCH — BRANCH_MANAGER: everything in their branch</li>
 *   <li>OWN    — RELATIONSHIP_OFFICER: records assigned to them (customers: read whole branch, see D-09)</li>
 * </ul>
 * Repositories receive a DataScope and add the matching WHERE clause, so a changed ID in the URL can
 * never reveal another branch's data.
 */
public record DataScope(Level level, Long branchId, Long userId) {

    public enum Level { ALL, BRANCH, OWN }

    public static DataScope of(AuthenticatedUser user) {
        return switch (user.role()) {
            case ADMIN -> new DataScope(Level.ALL, null, user.id());
            case BRANCH_MANAGER -> new DataScope(Level.BRANCH, requireBranch(user), user.id());
            case RELATIONSHIP_OFFICER -> new DataScope(Level.OWN, requireBranch(user), user.id());
        };
    }

    /** Branch-wide visibility (used for data an officer may read across the branch, e.g. customers). */
    public static DataScope branchRead(AuthenticatedUser user) {
        return user.role() == Role.ADMIN
                ? new DataScope(Level.ALL, null, user.id())
                : new DataScope(Level.BRANCH, requireBranch(user), user.id());
    }

    /** True if a record owned by {@code ownerId} in {@code recordBranchId} is inside this scope. */
    public boolean allows(Long recordBranchId, Long ownerId) {
        return switch (level) {
            case ALL -> true;
            case BRANCH -> branchId.equals(recordBranchId);
            case OWN -> branchId.equals(recordBranchId) && userId.equals(ownerId);
        };
    }

    private static Long requireBranch(AuthenticatedUser user) {
        if (user.branchId() == null) {
            throw new IllegalStateException("Non-admin user " + user.id() + " has no branch");
        }
        return user.branchId();
    }
}
