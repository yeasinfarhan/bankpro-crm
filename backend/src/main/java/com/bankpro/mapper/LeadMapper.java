package com.bankpro.mapper;

import com.bankpro.dto.LeadResponse;
import com.bankpro.dto.SaleResponse;
import com.bankpro.entity.Lead;
import com.bankpro.entity.LeadStatus;
import com.bankpro.entity.Sale;

import java.util.Set;

public final class LeadMapper {

    private LeadMapper() {
    }

    public static String code(Long id) {
        return id == null ? null : String.format("LD-%06d", id);
    }

    public static LeadResponse toResponse(Lead l, Set<LeadStatus> allowed, LeadResponse.Permissions permissions,
                                          SaleResponse sale) {
        return new LeadResponse(l.getId(), code(l.getId()),
                new SaleResponse.Ref(l.getCustomer().getId(), CustomerMapper.code(l.getCustomer().getId()),
                        l.getCustomer().getFullName()),
                new SaleResponse.Ref(l.getProduct().getId(), l.getProduct().getCode(), l.getProduct().getName()),
                l.getProduct().getCategory().name(), UserMapper.toResponse(l.getBranch()),
                CustomerMapper.summary(l.getAssignedOfficer()), l.getSource(), l.getStatus(), l.getPriority(),
                l.getEstimatedValue(), l.getExpectedCloseDate(), l.getLossReason(), l.getLossNote(), l.getNotes(),
                l.getLastContactedAt(), l.getStatusChangedAt(), l.getCreatedAt(), l.getUpdatedAt(), l.getVersion(),
                allowed, permissions, sale);
    }

    public static SaleResponse toResponse(Sale s, boolean canDecide) {
        return new SaleResponse(s.getId(), s.getLead().getId(), code(s.getLead().getId()),
                new SaleResponse.Ref(s.getCustomer().getId(), CustomerMapper.code(s.getCustomer().getId()),
                        s.getCustomer().getFullName()),
                new SaleResponse.Ref(s.getProduct().getId(), s.getProduct().getCode(), s.getProduct().getName()),
                s.getProduct().getCategory().name(), CustomerMapper.summary(s.getOfficer()), s.getAmount(),
                s.getStatus(), s.getConvertedAt(), s.getDecidedAt(), s.getDecisionNote(), canDecide);
    }
}
