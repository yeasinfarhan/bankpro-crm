package com.bankpro.repository;

import com.bankpro.entity.BankingProduct;
import com.bankpro.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BankingProductRepository extends JpaRepository<BankingProduct, Long> {

    boolean existsByCode(String code);

    /** Optional filters: a null parameter means "don't filter on this". Parameters are bound safely (no SQL injection). */
    @Query("""
            SELECT p FROM BankingProduct p
            WHERE (:active IS NULL OR p.active = :active)
              AND (:category IS NULL OR p.category = :category)
              AND (:q IS NULL OR LOWER(p.name) LIKE :q OR LOWER(p.code) LIKE :q)
            ORDER BY p.category, p.name
            """)
    List<BankingProduct> search(@Param("active") Boolean active,
                                @Param("category") ProductCategory category,
                                @Param("q") String likePattern);
}
