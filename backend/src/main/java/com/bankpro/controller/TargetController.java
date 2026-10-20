package com.bankpro.controller;

import com.bankpro.dto.TargetRequest;
import com.bankpro.dto.TargetResponse;
import com.bankpro.security.CurrentUserProvider;
import com.bankpro.service.TargetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/targets")
public class TargetController {

    private final TargetService targetService;
    private final CurrentUserProvider currentUser;

    public TargetController(TargetService targetService, CurrentUserProvider currentUser) {
        this.targetService = targetService;
        this.currentUser = currentUser;
    }

    /** Targets whose period overlaps [from, to] (inclusive dates). */
    @GetMapping
    public List<TargetResponse> list(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                     @RequestParam(required = false) Long branchId,
                                     @RequestParam(required = false) Long userId) {
        return targetService.list(currentUser.get(), from, to, branchId, userId);
    }

    @GetMapping("/ranking")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER')")
    public List<TargetResponse.Ranking> ranking(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                @RequestParam(required = false) Long branchId) {
        return targetService.ranking(currentUser.get(), from, to, branchId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('BRANCH_MANAGER')")
    public TargetResponse create(@Valid @RequestBody TargetRequest request, HttpServletRequest http) {
        return targetService.create(currentUser.get(), request, http.getRemoteAddr());
    }

    /** Body: {"targetValue": 500000} */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('BRANCH_MANAGER')")
    public TargetResponse update(@PathVariable Long id, @RequestBody Map<String, BigDecimal> body, HttpServletRequest http) {
        return targetService.update(currentUser.get(), id, body.get("targetValue"), http.getRemoteAddr());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('BRANCH_MANAGER')")
    public void delete(@PathVariable Long id, HttpServletRequest http) {
        targetService.delete(currentUser.get(), id, http.getRemoteAddr());
    }
}
