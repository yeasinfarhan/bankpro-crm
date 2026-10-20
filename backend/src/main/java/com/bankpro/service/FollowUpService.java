package com.bankpro.service;

import com.bankpro.audit.AuditAction;
import com.bankpro.audit.AuditService;
import com.bankpro.config.AppProperties;
import com.bankpro.dto.CompleteFollowUpRequest;
import com.bankpro.dto.FollowUpRequest;
import com.bankpro.dto.FollowUpResponse;
import com.bankpro.dto.PageResponse;
import com.bankpro.entity.AppUser;
import com.bankpro.entity.Customer;
import com.bankpro.entity.CustomerInteraction;
import com.bankpro.entity.FollowUpActivity;
import com.bankpro.entity.FollowUpStatus;
import com.bankpro.entity.InteractionType;
import com.bankpro.entity.Lead;
import com.bankpro.entity.Role;
import com.bankpro.exception.ConflictException;
import com.bankpro.exception.ForbiddenException;
import com.bankpro.exception.InvalidFieldException;
import com.bankpro.exception.NotFoundException;
import com.bankpro.mapper.FollowUpMapper;
import com.bankpro.repository.AppUserRepository;
import com.bankpro.repository.FollowUpRepository;
import com.bankpro.repository.FollowUpSpecifications;
import com.bankpro.repository.InteractionRepository;
import com.bankpro.security.AuthenticatedUser;
import com.bankpro.security.DataScope;
import com.bankpro.validation.ContactNormalizer;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Follow-up activities. "Today", "upcoming" and "overdue" are computed in Bangladesh time (Asia/Dhaka).
 * Officers see and change only their own; managers their branch; ADMIN read-only across branches.
 */
@Service
@Transactional
public class FollowUpService {

    public enum View { TODAY, UPCOMING, OVERDUE, COMPLETED, ALL }

    private static final Duration PAST_GRACE = Duration.ofMinutes(5);
    private static final Set<Role> OWNER_ROLES = Set.of(Role.RELATIONSHIP_OFFICER, Role.BRANCH_MANAGER);

    private final FollowUpRepository followUps;
    private final InteractionRepository interactions;
    private final AppUserRepository users;
    private final CustomerService customerService;
    private final LeadService leadService;
    private final AuditService audit;
    private final Clock clock;
    private final ZoneId zone;

    public FollowUpService(FollowUpRepository followUps, InteractionRepository interactions, AppUserRepository users,
                           CustomerService customerService, LeadService leadService, AuditService audit, Clock clock,
                           AppProperties props) {
        this.followUps = followUps;
        this.interactions = interactions;
        this.users = users;
        this.customerService = customerService;
        this.leadService = leadService;
        this.audit = audit;
        this.clock = clock;
        this.zone = props.businessZone();
    }

    /**
     * @param from/to inclusive Dhaka calendar dates; only used by the ALL view (calendar)
     */
    @Transactional(readOnly = true)
    public PageResponse<FollowUpResponse> list(AuthenticatedUser caller, View view, LocalDate from, LocalDate to,
                                               Long customerId, Long leadId, Long officerId, int page, int size) {
        Instant now = clock.instant();
        LocalDate today = LocalDate.now(clock.withZone(zone));
        Instant startToday = today.atStartOfDay(zone).toInstant();
        Instant startTomorrow = today.plusDays(1).atStartOfDay(zone).toInstant();

        FollowUpSpecifications.Filter filter;
        Sort sort = Sort.by("scheduledAt").ascending();
        switch (view) {
            case TODAY -> filter = filter(FollowUpStatus.SCHEDULED, startToday, startTomorrow, customerId, leadId, officerId);
            case UPCOMING -> filter = filter(FollowUpStatus.SCHEDULED, startTomorrow, null, customerId, leadId, officerId);
            case OVERDUE -> filter = filter(FollowUpStatus.SCHEDULED, null, now, customerId, leadId, officerId);
            case COMPLETED -> {
                filter = filter(FollowUpStatus.COMPLETED, null, null, customerId, leadId, officerId);
                sort = Sort.by("completedAt").descending();
            }
            default -> filter = filter(null,
                    from == null ? null : from.atStartOfDay(zone).toInstant(),
                    to == null ? null : to.plusDays(1).atStartOfDay(zone).toInstant(), customerId, leadId, officerId);
        }
        var spec = FollowUpSpecifications.matching(DataScope.of(caller), filter);
        var result = followUps.findAll(spec, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200),
                sort.and(Sort.by("id"))));
        return PageResponse.of(result, f -> FollowUpMapper.toResponse(f, now, canEdit(caller, f)));
    }

    @Transactional(readOnly = true)
    public FollowUpResponse.Counts counts(AuthenticatedUser caller) {
        Instant now = clock.instant();
        LocalDate today = LocalDate.now(clock.withZone(zone));
        Instant startToday = today.atStartOfDay(zone).toInstant();
        Instant startTomorrow = today.plusDays(1).atStartOfDay(zone).toInstant();
        DataScope scope = DataScope.of(caller);
        return new FollowUpResponse.Counts(
                followUps.count(FollowUpSpecifications.matching(scope,
                        filter(FollowUpStatus.SCHEDULED, startToday, startTomorrow, null, null, null))),
                followUps.count(FollowUpSpecifications.matching(scope,
                        filter(FollowUpStatus.SCHEDULED, startTomorrow, null, null, null, null))),
                followUps.count(FollowUpSpecifications.matching(scope,
                        filter(FollowUpStatus.SCHEDULED, null, now, null, null, null))));
    }

    public FollowUpResponse create(AuthenticatedUser caller, FollowUpRequest req, String ip) {
        if (caller.role() == Role.ADMIN) {
            throw new ForbiddenException("Administrators cannot schedule branch activities.");
        }
        Customer customer;
        try {
            customer = customerService.findVisible(caller, req.customerId());
        } catch (NotFoundException e) {
            throw new InvalidFieldException("customerId", "Choose a customer from your branch.");
        }
        if (customer.isArchived()) {
            throw new InvalidFieldException("customerId", "This customer is archived.");
        }
        Lead lead = null;
        if (req.leadId() != null) {
            try {
                lead = leadService.findVisible(caller, req.leadId());
            } catch (NotFoundException e) {
                throw new InvalidFieldException("leadId", "Choose one of your leads.");
            }
            if (!Objects.equals(lead.getCustomer().getId(), customer.getId())) {
                throw new InvalidFieldException("leadId", "This lead belongs to a different customer.");
            }
        }
        Instant when = futureInstant(req.scheduledAt());
        AppUser officer = resolveOwner(caller, req.assignedOfficerId());
        FollowUpActivity f = followUps.save(new FollowUpActivity(customer, lead, officer,
                users.getReferenceById(caller.id()), req.activityType(), when, ContactNormalizer.clean(req.notes())));
        return FollowUpMapper.toResponse(f, clock.instant(), canEdit(caller, f));
    }

    /** Completing writes the outcome into the customer's interaction history in the same transaction. */
    public FollowUpResponse complete(AuthenticatedUser caller, Long id, CompleteFollowUpRequest req, String ip) {
        FollowUpActivity f = findEditable(caller, id);
        Instant now = clock.instant();
        String outcome = ContactNormalizer.clean(req.outcome());
        String notes = ContactNormalizer.clean(req.notes());
        f.complete(outcome, notes, now);

        String summary = notes == null ? outcome : outcome + " — " + notes;
        interactions.save(new CustomerInteraction(f.getCustomer(), f.getLead(), f, users.getReferenceById(caller.id()),
                InteractionType.valueOf(f.getActivityType().name()), summary, now));
        f.getCustomer().touchInteraction(now);
        if (f.getLead() != null) {
            f.getLead().touchContact(now);
        }
        audit.record(caller.id(), AuditAction.FOLLOW_UP_COMPLETED, "FOLLOW_UP", id, Map.of(), ip);
        return FollowUpMapper.toResponse(f, now, false);
    }

    public FollowUpResponse reschedule(AuthenticatedUser caller, Long id, OffsetDateTime when) {
        FollowUpActivity f = findEditable(caller, id);
        f.reschedule(futureInstant(when));
        return FollowUpMapper.toResponse(f, clock.instant(), true);
    }

    // ---------------------------------------------------------------- helpers

    private FollowUpActivity findEditable(AuthenticatedUser caller, Long id) {
        DataScope scope = DataScope.of(caller);
        FollowUpActivity f = followUps.findWithDetailsById(id)
                .filter(x -> scope.allows(x.getBranch().getId(), x.getAssignedOfficer().getId()))
                .orElseThrow(() -> new NotFoundException("Follow-up not found."));
        if (f.getStatus() != FollowUpStatus.SCHEDULED) {
            throw new ConflictException("This follow-up is already " + f.getStatus().name().toLowerCase() + ".");
        }
        if (!canEdit(caller, f)) {
            throw new ForbiddenException("You can only change your own follow-ups.");
        }
        return f;
    }

    boolean canEdit(AuthenticatedUser caller, FollowUpActivity f) {
        if (f.getStatus() != FollowUpStatus.SCHEDULED) {
            return false;
        }
        boolean sameBranch = Objects.equals(caller.branchId(), f.getBranch().getId());
        return (caller.role() == Role.BRANCH_MANAGER && sameBranch)
                || (caller.role() == Role.RELATIONSHIP_OFFICER && Objects.equals(caller.id(), f.getAssignedOfficer().getId()));
    }

    private Instant futureInstant(OffsetDateTime when) {
        Instant instant = when.toInstant();
        if (instant.isBefore(clock.instant().minus(PAST_GRACE))) {
            throw new InvalidFieldException("scheduledAt",
                    "Choose a future date and time. To record something that already happened, log an interaction.");
        }
        return instant;
    }

    private AppUser resolveOwner(AuthenticatedUser caller, Long requestedId) {
        if (caller.role() == Role.RELATIONSHIP_OFFICER || requestedId == null) {
            return users.getReferenceById(caller.id());
        }
        return users.findWithBranchById(requestedId)
                .filter(AppUser::isActive)
                .filter(u -> OWNER_ROLES.contains(u.getRole()))
                .filter(u -> u.getBranch() != null && Objects.equals(u.getBranch().getId(), caller.branchId()))
                .orElseThrow(() -> new InvalidFieldException("assignedOfficerId", "Choose an active officer from your branch."));
    }

    private static FollowUpSpecifications.Filter filter(FollowUpStatus status, Instant from, Instant to,
                                                        Long customerId, Long leadId, Long officerId) {
        return new FollowUpSpecifications.Filter(status, from, to, customerId, leadId, officerId);
    }
}
