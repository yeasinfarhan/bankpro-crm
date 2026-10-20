package com.bankpro.metrics;

import com.bankpro.entity.Role;
import com.bankpro.security.AuthenticatedUser;

/**
 * Which slice of data a dashboard or report may aggregate, derived from the caller — never trusted from input:
 * <ul>
 *   <li>RELATIONSHIP_OFFICER: their own branch AND only their own records (officerId = self)</li>
 *   <li>BRANCH_MANAGER: their own branch; may narrow to one officer</li>
 *   <li>ADMIN: all branches, or the branch / officer they choose</li>
 * </ul>
 * A null field means "no restriction on this dimension".
 */
public record MetricScope(Long branchId, Long officerId) {

    public static MetricScope of(AuthenticatedUser caller, Long requestedBranchId, Long requestedOfficerId) {
        return switch (caller.role()) {
            case ADMIN -> new MetricScope(requestedBranchId, requestedOfficerId);
            case BRANCH_MANAGER -> new MetricScope(caller.branchId(), requestedOfficerId);
            case RELATIONSHIP_OFFICER -> new MetricScope(caller.branchId(), caller.id());
        };
    }

    public boolean isOfficerOnly(AuthenticatedUser caller) {
        return caller.role() == Role.RELATIONSHIP_OFFICER;
    }
}
