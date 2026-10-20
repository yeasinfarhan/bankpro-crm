package com.bankpro.repository;

import com.bankpro.entity.CustomerInteraction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InteractionRepository extends JpaRepository<CustomerInteraction, Long> {

    @EntityGraph(attributePaths = {"recordedBy", "lead"})
    List<CustomerInteraction> findByCustomerIdOrderByOccurredAtDescIdDesc(Long customerId, Pageable pageable);
}
