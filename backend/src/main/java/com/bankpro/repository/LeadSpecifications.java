package com.bankpro.repository;

import com.bankpro.entity.Lead;
import com.bankpro.entity.LeadStatus;
import com.bankpro.security.DataScope;
import com.bankpro.validation.SearchText;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** WHERE clause for lead queries. The security scope is always applied first; filters can only narrow it. */
public final class LeadSpecifications {

    private static final Pattern CODE = Pattern.compile("^LD-?0*(\\d+)$", Pattern.CASE_INSENSITIVE);

    private LeadSpecifications() {
    }

    /**
     * @param createdFrom inclusive, createdTo exclusive (already converted from Asia/Dhaka dates to UTC instants)
     */
    public record Filter(String q, List<LeadStatus> statuses, Long productId, Long officerId, Long customerId,
                         Long branchId, Instant createdFrom, Instant createdTo) {
    }

    public static Specification<Lead> matching(DataScope scope, Filter f) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            switch (scope.level()) {
                case ALL -> { }
                case BRANCH -> p.add(cb.equal(root.get("branch").get("id"), scope.branchId()));
                case OWN -> {
                    p.add(cb.equal(root.get("branch").get("id"), scope.branchId()));
                    p.add(cb.equal(root.get("assignedOfficer").get("id"), scope.userId()));
                }
            }
            if (f.branchId() != null) p.add(cb.equal(root.get("branch").get("id"), f.branchId()));
            if (f.statuses() != null && !f.statuses().isEmpty()) p.add(root.get("status").in(f.statuses()));
            if (f.productId() != null) p.add(cb.equal(root.get("product").get("id"), f.productId()));
            if (f.officerId() != null) p.add(cb.equal(root.get("assignedOfficer").get("id"), f.officerId()));
            if (f.customerId() != null) p.add(cb.equal(root.get("customer").get("id"), f.customerId()));
            if (f.createdFrom() != null) p.add(cb.greaterThanOrEqualTo(root.get("createdAt"), f.createdFrom()));
            if (f.createdTo() != null) p.add(cb.lessThan(root.get("createdAt"), f.createdTo()));
            if (f.q() != null && !f.q().isBlank()) {
                Matcher code = CODE.matcher(f.q().trim());
                if (code.matches()) {
                    p.add(cb.equal(root.get("id"), Long.valueOf(code.group(1))));
                } else {
                    String like = SearchText.containsPattern(f.q());
                    p.add(cb.or(cb.like(cb.lower(root.get("customer").get("fullName")), like),
                            cb.like(cb.lower(root.get("product").get("name")), like)));
                }
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }
}
