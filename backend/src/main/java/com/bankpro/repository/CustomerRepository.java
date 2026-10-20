package com.bankpro.repository;

import com.bankpro.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long>, JpaSpecificationExecutor<Customer> {

    /** Loads branch + officer in the same query to avoid one extra query per row ("N+1 problem"). */
    @Override
    @EntityGraph(attributePaths = {"branch", "assignedOfficer"})
    Page<Customer> findAll(Specification<Customer> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"branch", "assignedOfficer"})
    Optional<Customer> findWithDetailsById(Long id);

    @Query("SELECT c.id FROM Customer c WHERE c.email = :email")
    Optional<Long> findIdByEmail(String email);

    @Query("SELECT c.id FROM Customer c WHERE c.phone = :phone")
    Optional<Long> findIdByPhone(String phone);
}
