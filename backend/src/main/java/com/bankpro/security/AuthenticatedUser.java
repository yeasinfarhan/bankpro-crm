package com.bankpro.security;

import com.bankpro.entity.Role;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Who is calling the API, read from the verified JWT claims.
 * Services use this, never IDs sent by the client, to decide what data the caller may see.
 *
 * @param branchId null only for ADMIN
 */
public record AuthenticatedUser(Long id, String email, String fullName, Role role, Long branchId) {

    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_NAME = "name";
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_BRANCH = "branchId";

    public static AuthenticatedUser fromJwt(Jwt jwt) {
        Object branch = jwt.getClaim(CLAIM_BRANCH);
        Long branchId = branch instanceof Number n ? n.longValue() : null;
        return new AuthenticatedUser(
                Long.valueOf(jwt.getSubject()),
                jwt.getClaimAsString(CLAIM_EMAIL),
                jwt.getClaimAsString(CLAIM_NAME),
                Role.valueOf(jwt.getClaimAsString(CLAIM_ROLE)),
                branchId);
    }

    public boolean hasRole(Role r) {
        return role == r;
    }
}
