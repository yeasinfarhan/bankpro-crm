package com.bankpro.repository;

import com.bankpro.entity.Sale;
import com.bankpro.entity.SaleStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    Optional<Sale> findByLeadId(Long leadId);

    @EntityGraph(attributePaths = {"customer", "product", "branch", "officer", "lead"})
    Optional<Sale> findWithDetailsById(Long id);

    /** Scoped list: branchId null = all branches; officerId null = all officers in scope. */
    @EntityGraph(attributePaths = {"customer", "product", "branch", "officer", "lead"})
    @Query("""
            SELECT s FROM Sale s
            WHERE (:branchId IS NULL OR s.branch.id = :branchId)
              AND (:officerId IS NULL OR s.officer.id = :officerId)
              AND (:status IS NULL OR s.status = :status)
            ORDER BY s.convertedAt DESC
            """)
    List<Sale> search(@Param("branchId") Long branchId, @Param("officerId") Long officerId,
                      @Param("status") SaleStatus status);
}
