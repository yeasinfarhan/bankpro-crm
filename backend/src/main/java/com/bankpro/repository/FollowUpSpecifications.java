package com.bankpro.repository;

import com.bankpro.entity.FollowUpActivity;
import com.bankpro.entity.FollowUpStatus;
import com.bankpro.security.DataScope;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** WHERE clause for follow-up queries; scope first, then filters. */
public final class FollowUpSpecifications {

    private FollowUpSpecifications() {
    }

    /** All bounds are UTC instants: scheduled in [from, to). */
    public record Filter(FollowUpStatus status, Instant scheduledFrom, Instant scheduledTo, Long customerId,
                         Long leadId, Long officerId) {
    }

    public static Specification<FollowUpActivity> matching(DataScope scope, Filter f) {
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
            if (f.status() != null) p.add(cb.equal(root.get("status"), f.status()));
            if (f.scheduledFrom() != null) p.add(cb.greaterThanOrEqualTo(root.get("scheduledAt"), f.scheduledFrom()));
            if (f.scheduledTo() != null) p.add(cb.lessThan(root.get("scheduledAt"), f.scheduledTo()));
            if (f.customerId() != null) p.add(cb.equal(root.get("customer").get("id"), f.customerId()));
            if (f.leadId() != null) p.add(cb.equal(root.get("lead").get("id"), f.leadId()));
            if (f.officerId() != null) p.add(cb.equal(root.get("assignedOfficer").get("id"), f.officerId()));
            return cb.and(p.toArray(Predicate[]::new));
        };
    }
}
