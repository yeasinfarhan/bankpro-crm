package com.bankpro.repository;

import com.bankpro.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data generates the implementation (findAll, findById, ...) at runtime. */
public interface BranchRepository extends JpaRepository<Branch, Long> {
}
