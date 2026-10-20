package com.bankpro.mapper;

import com.bankpro.dto.ProductResponse;
import com.bankpro.entity.BankingProduct;

import java.util.List;

public final class ProductMapper {

    private ProductMapper() {
    }

    public static ProductResponse toResponse(BankingProduct p) {
        return new ProductResponse(p.getId(), p.getCode(), p.getName(), p.getCategory(), p.getDescription(),
                p.getEligibilityNotes(), p.getBenefits() == null ? List.of() : List.copyOf(p.getBenefits()),
                p.getIllustrativeRate(), p.isActive(), p.getCreatedAt(), p.getUpdatedAt());
    }
}
