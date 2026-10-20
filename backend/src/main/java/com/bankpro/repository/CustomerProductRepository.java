package com.bankpro.repository;

import com.bankpro.entity.CustomerProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CustomerProductRepository extends JpaRepository<CustomerProduct, Long> {

    @Query("SELECT cp FROM CustomerProduct cp JOIN FETCH cp.product WHERE cp.customer.id = :customerId ORDER BY cp.sinceDate")
    List<CustomerProduct> findByCustomerIdWithProduct(Long customerId);

    boolean existsByCustomerId(Long customerId);

    /** Removes only manually recorded holdings; holdings created by sales are history and stay. */
    @Modifying
    @Query("DELETE FROM CustomerProduct cp WHERE cp.customer.id = :customerId AND cp.sourceSaleId IS NULL")
    void deleteManualByCustomerId(Long customerId);
}
