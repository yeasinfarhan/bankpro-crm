package com.bankpro.repository;

import com.bankpro.entity.Customer;
import com.bankpro.entity.CustomerSegment;
import com.bankpro.entity.CustomerStatus;
import com.bankpro.security.DataScope;
import com.bankpro.validation.SearchText;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds the WHERE clause for customer searches from optional filters.
 * The scope predicate is ALWAYS added first, so filters can only narrow what the caller may see.
 */
public final class CustomerSpecifications {

    private static final Pattern CODE = Pattern.compile("^CUS-?0*(\\d+)$", Pattern.CASE_INSENSITIVE);

    private CustomerSpecifications() {
    }

    public record Filter(String q, CustomerSegment segment, CustomerStatus status, String occupation,
                         Long officerId, Long branchId, boolean includeArchived) {
    }

    public static Specification<Customer> matching(DataScope scope, Filter f) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();

            // 1. Security scope
            switch (scope.level()) {
                case ALL -> { }
                case BRANCH -> p.add(cb.equal(root.get("branch").get("id"), scope.branchId()));
                case OWN -> {
                    p.add(cb.equal(root.get("branch").get("id"), scope.branchId()));
                    p.add(cb.equal(root.get("assignedOfficer").get("id"), scope.userId()));
                }
            }

            // 2. Optional filters
            if (f.branchId() != null) {
                p.add(cb.equal(root.get("branch").get("id"), f.branchId()));
            }
            if (f.status() != null) {
                p.add(cb.equal(root.get("status"), f.status()));
            } else if (!f.includeArchived()) {
                p.add(cb.notEqual(root.get("status"), CustomerStatus.ARCHIVED));
            }
            if (f.segment() != null) {
                p.add(cb.equal(root.get("segment"), f.segment()));
            }
            if (f.officerId() != null) {
                p.add(cb.equal(root.get("assignedOfficer").get("id"), f.officerId()));
            }
            if (f.occupation() != null && !f.occupation().isBlank()) {
                p.add(cb.like(cb.lower(root.get("occupation")), like(f.occupation())));
            }
            if (f.q() != null && !f.q().isBlank()) {
                String term = f.q().trim();
                Matcher code = CODE.matcher(term);
                if (code.matches()) {
                    p.add(cb.equal(root.get("id"), Long.valueOf(code.group(1))));
                } else {
                    String pattern = like(term);
                    String digits = term.replaceAll("\\D", "");
                    List<Predicate> any = new ArrayList<>(List.of(
                            cb.like(cb.lower(root.get("fullName")), pattern),
                            cb.like(cb.lower(root.get("email")), pattern)));
                    if (digits.length() >= 4) {
                        any.add(cb.like(root.get("phone"), "%" + digits + "%"));
                    }
                    p.add(cb.or(any.toArray(Predicate[]::new)));
                }
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }

    /** Escapes LIKE wildcards in user input so "50%" searches for the text "50%". */
    static String like(String term) {
        return SearchText.containsPattern(term);
    }
}
