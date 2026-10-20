package com.bankpro.repository;

import com.bankpro.entity.Lead;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LeadRepository extends JpaRepository<Lead, Long>, JpaSpecificationExecutor<Lead>, LeadAggregates {

    String[] DETAILS = {"customer", "product", "branch", "assignedOfficer"};

    @Override
    @EntityGraph(attributePaths = {"customer", "product", "branch", "assignedOfficer"})
    Page<Lead> findAll(Specification<Lead> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"customer", "product", "branch", "assignedOfficer"})
    List<Lead> findAll(Specification<Lead> spec, org.springframework.data.domain.Sort sort);

    @EntityGraph(attributePaths = {"customer", "product", "branch", "assignedOfficer"})
    Optional<Lead> findWithDetailsById(Long id);

    /**
     * SELECT ... FOR UPDATE: a second request converting the same lead waits here until the first
     * transaction commits, then sees status CONVERTED. This is what makes conversion safe under double-clicks.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM Lead l WHERE l.id = :id")
    Optional<Lead> findByIdForUpdate(@Param("id") Long id);
}
