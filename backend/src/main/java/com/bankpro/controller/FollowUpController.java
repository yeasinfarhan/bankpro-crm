package com.bankpro.controller;

import com.bankpro.dto.CompleteFollowUpRequest;
import com.bankpro.dto.FollowUpRequest;
import com.bankpro.dto.FollowUpResponse;
import com.bankpro.dto.PageResponse;
import com.bankpro.dto.RescheduleRequest;
import com.bankpro.security.CurrentUserProvider;
import com.bankpro.service.FollowUpService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/follow-ups")
public class FollowUpController {

    private final FollowUpService followUpService;
    private final CurrentUserProvider currentUser;

    public FollowUpController(FollowUpService followUpService, CurrentUserProvider currentUser) {
        this.followUpService = followUpService;
        this.currentUser = currentUser;
    }

    /** view = TODAY | UPCOMING | OVERDUE | COMPLETED | ALL (ALL + from/to powers the calendar). */
    @GetMapping
    public PageResponse<FollowUpResponse> list(
            @RequestParam(defaultValue = "TODAY") FollowUpService.View view,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long leadId,
            @RequestParam(required = false) Long officerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return followUpService.list(currentUser.get(), view, from, to, customerId, leadId, officerId, page, size);
    }

    @GetMapping("/counts")
    public FollowUpResponse.Counts counts() {
        return followUpService.counts(currentUser.get());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER', 'RELATIONSHIP_OFFICER')")
    public FollowUpResponse create(@Valid @RequestBody FollowUpRequest request, HttpServletRequest http) {
        return followUpService.create(currentUser.get(), request, http.getRemoteAddr());
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER', 'RELATIONSHIP_OFFICER')")
    public FollowUpResponse complete(@PathVariable Long id, @Valid @RequestBody CompleteFollowUpRequest request,
                                     HttpServletRequest http) {
        return followUpService.complete(currentUser.get(), id, request, http.getRemoteAddr());
    }

    @PatchMapping("/{id}/reschedule")
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER', 'RELATIONSHIP_OFFICER')")
    public FollowUpResponse reschedule(@PathVariable Long id, @Valid @RequestBody RescheduleRequest request) {
        return followUpService.reschedule(currentUser.get(), id, request.scheduledAt());
    }
}
