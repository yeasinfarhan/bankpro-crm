package com.bankpro.service;

import com.bankpro.dto.InteractionRequest;
import com.bankpro.dto.TimelineItem;
import com.bankpro.entity.Customer;
import com.bankpro.entity.CustomerInteraction;
import com.bankpro.entity.FollowUpStatus;
import com.bankpro.entity.Lead;
import com.bankpro.entity.Role;
import com.bankpro.exception.ForbiddenException;
import com.bankpro.exception.InvalidFieldException;
import com.bankpro.exception.NotFoundException;
import com.bankpro.mapper.LeadMapper;
import com.bankpro.repository.AppUserRepository;
import com.bankpro.repository.FollowUpRepository;
import com.bankpro.repository.InteractionRepository;
import com.bankpro.repository.LeadRepository;
import com.bankpro.repository.LeadSpecifications;
import com.bankpro.repository.SaleRepository;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Interaction log and the customer's chronological activity history. */
@Service
@Transactional
public class InteractionService {

    private static final int TIMELINE_LIMIT = 100;

    private final InteractionRepository interactions;
    private final FollowUpRepository followUps;
    private final LeadRepository leads;
    private final SaleRepository sales;
    private final AppUserRepository users;
    private final CustomerService customerService;
    private final LeadService leadService;
    private final Clock clock;

    public InteractionService(InteractionRepository interactions, FollowUpRepository followUps, LeadRepository leads,
                              SaleRepository sales, AppUserRepository users, CustomerService customerService,
                              LeadService leadService, Clock clock) {
        this.interactions = interactions;
        this.followUps = followUps;
        this.leads = leads;
        this.sales = sales;
        this.users = users;
        this.customerService = customerService;
        this.leadService = leadService;
        this.clock = clock;
    }

    /** Any officer or manager who can see the customer (same branch) may log what happened. */
    public TimelineItem record(AuthenticatedUser caller, Long customerId, InteractionRequest req) {
        if (caller.role() == Role.ADMIN) {
            throw new ForbiddenException("Administrators cannot record branch interactions.");
        }
        Customer customer = customerService.findVisible(caller, customerId);
        Instant now = clock.instant();
        Instant occurred = req.occurredAt() == null ? now : req.occurredAt().toInstant();
        if (occurred.isAfter(now.plus(Duration.ofMinutes(1)))) {
            throw new InvalidFieldException("occurredAt", "An interaction cannot be in the future. Schedule a follow-up instead.");
        }
        Lead lead = null;
        if (req.leadId() != null) {
            lead = leadService.findVisible(caller, req.leadId());
            if (!Objects.equals(lead.getCustomer().getId(), customerId)) {
                throw new InvalidFieldException("leadId", "This lead belongs to a different customer.");
            }
            lead.touchContact(occurred);
        }
        CustomerInteraction saved = interactions.save(new CustomerInteraction(customer, lead, null,
                users.getReferenceById(caller.id()), req.interactionType(), ContactNormalizer.clean(req.summary()), occurred));
        customer.touchInteraction(occurred);
        return interactionItem(saved, caller.fullName());
    }

    /** Newest first: interactions, scheduled follow-ups, leads opened/closed and sales — up to 100 entries. */
    @Transactional(readOnly = true)
    public List<TimelineItem> timeline(AuthenticatedUser caller, Long customerId) {
        customerService.findVisible(caller, customerId); // 404 if out of scope
        List<TimelineItem> items = new ArrayList<>();

        interactions.findByCustomerIdOrderByOccurredAtDescIdDesc(customerId, PageRequest.of(0, TIMELINE_LIMIT))
                .forEach(i -> items.add(interactionItem(i, i.getRecordedBy().getFullName())));

        followUps.findByCustomerIdOrderByScheduledAtDesc(customerId).stream()
                .filter(f -> f.getStatus() == FollowUpStatus.SCHEDULED)
                .forEach(f -> items.add(new TimelineItem("FOLLOW_UP_SCHEDULED", f.getScheduledAt(),
                        label(f.getActivityType().name()) + " scheduled", f.getNotes(),
                        f.getAssignedOfficer().getFullName(), "/follow-ups")));

        var leadFilter = new LeadSpecifications.Filter(null, null, null, null, customerId, null, null, null);
        leads.findAll(LeadSpecifications.matching(DataScope.branchRead(caller), leadFilter), Sort.by("createdAt"))
                .forEach(l -> {
                    items.add(new TimelineItem("LEAD_CREATED", l.getCreatedAt(),
                            "Lead opened: " + l.getProduct().getName(), LeadMapper.code(l.getId()),
                            l.getAssignedOfficer().getFullName(), "/leads/" + l.getId()));
                    if (!l.isOpen()) {
                        String detail = l.getLossReason() == null ? null : "Reason: " + label(l.getLossReason().name());
                        items.add(new TimelineItem("LEAD_CLOSED", l.getStatusChangedAt(),
                                "Lead " + l.getStatus().name().toLowerCase(Locale.ROOT) + ": " + l.getProduct().getName(),
                                detail, l.getAssignedOfficer().getFullName(), "/leads/" + l.getId()));
                    }
                    sales.findByLeadId(l.getId()).ifPresent(s -> items.add(new TimelineItem("SALE", s.getConvertedAt(),
                            "Simulated sale: " + l.getProduct().getName(),
                            "Status: " + label(s.getStatus().name()), l.getAssignedOfficer().getFullName(),
                            "/leads/" + l.getId())));
                });

        items.sort(Comparator.comparing(TimelineItem::at).reversed());
        return items.size() > TIMELINE_LIMIT ? items.subList(0, TIMELINE_LIMIT) : items;
    }

    private static TimelineItem interactionItem(CustomerInteraction i, String actor) {
        String link = i.getLead() == null ? null : "/leads/" + i.getLead().getId();
        return new TimelineItem("INTERACTION", i.getOccurredAt(), label(i.getInteractionType().name()), i.getSummary(),
                actor, link);
    }

    /** PHONE_CALL → "Phone call". */
    static String label(String enumName) {
        String s = enumName.replace('_', ' ').toLowerCase(Locale.ROOT);
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
