package com.bankpro.repository;

import com.bankpro.entity.Lead;
import com.bankpro.entity.LeadStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

/**
 * Spring Data picks this class up automatically because its name is the fragment interface name + "Impl".
 * It reuses the same Specification as the list endpoint, so totals always match what the user can see.
 */
class LeadAggregatesImpl implements LeadAggregates {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Map<LeadStatus, StatusTotals> totalsByStatus(Specification<Lead> spec) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> q = cb.createTupleQuery();
        Root<Lead> root = q.from(Lead.class);
        q.multiselect(root.get("status"), cb.count(root), cb.coalesce(cb.sum(root.<BigDecimal>get("estimatedValue")), BigDecimal.ZERO));
        if (spec != null) {
            q.where(spec.toPredicate(root, q, cb));
        }
        q.groupBy(root.get("status"));

        Map<LeadStatus, StatusTotals> result = new EnumMap<>(LeadStatus.class);
        for (LeadStatus s : LeadStatus.values()) {
            result.put(s, new StatusTotals(0, BigDecimal.ZERO));
        }
        for (Tuple t : em.createQuery(q).getResultList()) {
            result.put(t.get(0, LeadStatus.class),
                    new StatusTotals(t.get(1, Long.class), new BigDecimal(t.get(2).toString())));
        }
        return result;
    }
}
