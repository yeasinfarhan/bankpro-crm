package com.bankpro.repository;

import com.bankpro.entity.PeriodType;
import com.bankpro.entity.SalesTarget;
import com.bankpro.entity.TargetCategory;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SalesTargetRepository extends JpaRepository<SalesTarget, Long> {

    /**
     * Targets whose period overlaps [from, to]. branchId null = all branches.
     * officerOnly: the officer's own targets plus their branch-level targets.
     */
    @EntityGraph(attributePaths = {"branch", "user"})
    @Query("""
            SELECT t FROM SalesTarget t LEFT JOIN t.user u
            WHERE t.periodStart <= :to AND t.periodEnd >= :from
              AND (:branchId IS NULL OR t.branch.id = :branchId)
              AND (:userId IS NULL OR u.id = :userId OR (:includeBranchTargets = TRUE AND u IS NULL))
            ORDER BY t.periodStart DESC, t.branch.name, u.fullName, t.category
            """)
    List<SalesTarget> findOverlapping(@Param("from") LocalDate from, @Param("to") LocalDate to,
                                      @Param("branchId") Long branchId, @Param("userId") Long userId,
                                      @Param("includeBranchTargets") boolean includeBranchTargets);

    @EntityGraph(attributePaths = {"branch", "user"})
    Optional<SalesTarget> findWithDetailsById(Long id);

    /** Same owner (officer, or the whole branch when userId is null), period and category. */
    @Query("""
            SELECT COUNT(t) > 0 FROM SalesTarget t LEFT JOIN t.user u
            WHERE t.branch.id = :branchId
              AND ((:userId IS NULL AND u IS NULL) OR u.id = :userId)
              AND t.periodType = :periodType AND t.periodStart = :periodStart AND t.category = :category
            """)
    boolean existsSame(@Param("branchId") Long branchId, @Param("userId") Long userId,
                       @Param("periodType") PeriodType periodType, @Param("periodStart") LocalDate periodStart,
                       @Param("category") TargetCategory category);
}
