package com.bankpro.repository;

import com.bankpro.entity.AppUser;
import com.bankpro.entity.Role;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    /** Emails are stored lower-case, so callers normalise before calling. Loads the branch in the same query. */
    @EntityGraph(attributePaths = "branch")
    Optional<AppUser> findByEmail(String email);

    @EntityGraph(attributePaths = "branch")
    Optional<AppUser> findWithBranchById(Long id);

    @EntityGraph(attributePaths = "branch")
    List<AppUser> findAllByOrderByFullNameAsc();

    @EntityGraph(attributePaths = "branch")
    List<AppUser> findByBranchIdOrderByFullNameAsc(Long branchId);

    @EntityGraph(attributePaths = "branch")
    List<AppUser> findByBranchIdAndRoleAndActiveTrueOrderByFullNameAsc(Long branchId, Role role);

    /** Active staff who can own customers/leads (officers and managers). */
    @EntityGraph(attributePaths = "branch")
    List<AppUser> findByBranchIdAndActiveTrueAndRoleInOrderByFullNameAsc(Long branchId, Collection<Role> roles);

    @EntityGraph(attributePaths = "branch")
    List<AppUser> findByActiveTrueAndRoleInOrderByFullNameAsc(Collection<Role> roles);
}
