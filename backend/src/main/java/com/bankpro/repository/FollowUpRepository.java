package com.bankpro.repository;

import com.bankpro.entity.FollowUpActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface FollowUpRepository extends JpaRepository<FollowUpActivity, Long>, JpaSpecificationExecutor<FollowUpActivity> {

    @Override
    @EntityGraph(attributePaths = {"customer", "lead", "branch", "assignedOfficer"})
    Page<FollowUpActivity> findAll(Specification<FollowUpActivity> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "lead", "branch", "assignedOfficer"})
    Optional<FollowUpActivity> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"assignedOfficer", "lead"})
    List<FollowUpActivity> findByCustomerIdOrderByScheduledAtDesc(Long customerId);

    /** Overdue activities, oldest first, for the reminder job. */
    @EntityGraph(attributePaths = {"customer", "assignedOfficer"})
    @Query("""
            SELECT f FROM FollowUpActivity f
            WHERE f.status = com.bankpro.entity.FollowUpStatus.SCHEDULED AND f.scheduledAt < :now
            ORDER BY f.scheduledAt
            """)
    List<FollowUpActivity> findOverdue(@Param("now") Instant now, Pageable pageable);
}
