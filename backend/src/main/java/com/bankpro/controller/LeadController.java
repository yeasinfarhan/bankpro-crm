package com.bankpro.controller;

import com.bankpro.config.AppProperties;
import com.bankpro.dto.AssignRequest;
import com.bankpro.dto.ConversionResponse;
import com.bankpro.dto.ConvertLeadRequest;
import com.bankpro.dto.LeadRequest;
import com.bankpro.dto.LeadResponse;
import com.bankpro.dto.LeadStatusRequest;
import com.bankpro.dto.LeadSummaryResponse;
import com.bankpro.dto.PageResponse;
import com.bankpro.entity.LeadStatus;
import com.bankpro.repository.LeadSpecifications;
import com.bankpro.security.CurrentUserProvider;
import com.bankpro.service.LeadService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leads")
public class LeadController {

    private final LeadService leadService;
    private final CurrentUserProvider currentUser;
    private final AppProperties props;

    public LeadController(LeadService leadService, CurrentUserProvider currentUser, AppProperties props) {
        this.leadService = leadService;
        this.currentUser = currentUser;
        this.props = props;
    }

    /** {@code from}/{@code to} are inclusive calendar dates in Bangladesh time (filter on creation date). */
    private LeadSpecifications.Filter filter(String q, List<LeadStatus> status, Long productId, Long officerId,
                                             Long customerId, Long branchId, LocalDate from, LocalDate to) {
        var zone = props.businessZone();
        return new LeadSpecifications.Filter(q, status, productId, officerId, customerId, branchId,
                from == null ? null : from.atStartOfDay(zone).toInstant(),
                to == null ? null : to.plusDays(1).atStartOfDay(zone).toInstant());
    }

    @GetMapping
    public PageResponse<LeadResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) List<LeadStatus> status,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long officerId,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {
        return leadService.search(currentUser.get(),
                filter(q, status, productId, officerId, customerId, branchId, from, to), page, size, sort);
    }

    @GetMapping("/board")
    public Map<LeadStatus, LeadService.BoardColumn> board(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long officerId,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return leadService.board(currentUser.get(), filter(q, null, productId, officerId, null, branchId, from, to));
    }

    @GetMapping("/summary")
    public LeadSummaryResponse summary(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long officerId,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return leadService.summary(currentUser.get(), filter(q, null, productId, officerId, null, branchId, from, to));
    }

    @GetMapping("/{id}")
    public LeadResponse get(@PathVariable Long id) {
        return leadService.get(currentUser.get(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER', 'RELATIONSHIP_OFFICER')")
    public LeadResponse create(@Valid @RequestBody LeadRequest request, HttpServletRequest http) {
        return leadService.create(currentUser.get(), request, http.getRemoteAddr());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER', 'RELATIONSHIP_OFFICER')")
    public LeadResponse update(@PathVariable Long id, @Valid @RequestBody LeadRequest request, HttpServletRequest http) {
        return leadService.update(currentUser.get(), id, request, http.getRemoteAddr());
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER', 'RELATIONSHIP_OFFICER')")
    public LeadResponse changeStatus(@PathVariable Long id, @Valid @RequestBody LeadStatusRequest request,
                                     HttpServletRequest http) {
        return leadService.changeStatus(currentUser.get(), id, request, http.getRemoteAddr());
    }

    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasRole('BRANCH_MANAGER')")
    public LeadResponse assign(@PathVariable Long id, @Valid @RequestBody AssignRequest request, HttpServletRequest http) {
        return leadService.assign(currentUser.get(), id, request.officerId(), http.getRemoteAddr());
    }

    /** 201 Created for a new sale; 200 OK with the existing sale if the lead was already converted. */
    @PostMapping("/{id}/convert")
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER', 'RELATIONSHIP_OFFICER')")
    public ResponseEntity<ConversionResponse> convert(@PathVariable Long id, @Valid @RequestBody ConvertLeadRequest request,
                                                      HttpServletRequest http) {
        ConversionResponse result = leadService.convert(currentUser.get(), id, request.amount(), http.getRemoteAddr());
        return ResponseEntity.status(result.alreadyConverted() ? HttpStatus.OK : HttpStatus.CREATED).body(result);
    }
}
