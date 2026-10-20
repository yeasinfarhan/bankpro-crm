package com.bankpro.repository;

import com.bankpro.entity.Lead;
import com.bankpro.entity.LeadStatus;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.Map;

/** Aggregations computed by the database (COUNT/SUM GROUP BY), never by loading every lead into memory. */
public interface LeadAggregates {

    record StatusTotals(long count, BigDecimal estimatedValue) {
    }

    Map<LeadStatus, StatusTotals> totalsByStatus(Specification<Lead> spec);
}
