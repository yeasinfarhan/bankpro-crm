package com.bankpro.controller;

import com.bankpro.dto.SaleDecisionRequest;
import com.bankpro.dto.SaleResponse;
import com.bankpro.entity.SaleStatus;
import com.bankpro.security.CurrentUserProvider;
import com.bankpro.service.LeadService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Simulated sales. They are never deleted (they are business history). */
@RestController
@RequestMapping("/api/sales")
public class SaleController {

    private final LeadService leadService;
    private final CurrentUserProvider currentUser;

    public SaleController(LeadService leadService, CurrentUserProvider currentUser) {
        this.leadService = leadService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<SaleResponse> list(@RequestParam(required = false) SaleStatus status) {
        return leadService.listSales(currentUser.get(), status);
    }

    @PatchMapping("/{id}/decision")
    @PreAuthorize("hasRole('BRANCH_MANAGER')")
    public SaleResponse decide(@PathVariable Long id, @Valid @RequestBody SaleDecisionRequest request,
                               HttpServletRequest http) {
        return leadService.decide(currentUser.get(), id, request, http.getRemoteAddr());
    }
}
