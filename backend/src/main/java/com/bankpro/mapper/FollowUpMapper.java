package com.bankpro.mapper;

import com.bankpro.dto.FollowUpResponse;
import com.bankpro.dto.SaleResponse;
import com.bankpro.entity.FollowUpActivity;

import java.time.Instant;

public final class FollowUpMapper {

    private FollowUpMapper() {
    }

    public static FollowUpResponse toResponse(FollowUpActivity f, Instant now, boolean canEdit) {
        SaleResponse.Ref lead = f.getLead() == null ? null
                : new SaleResponse.Ref(f.getLead().getId(), LeadMapper.code(f.getLead().getId()), null);
        return new FollowUpResponse(f.getId(),
                new SaleResponse.Ref(f.getCustomer().getId(), CustomerMapper.code(f.getCustomer().getId()),
                        f.getCustomer().getFullName()),
                lead, CustomerMapper.summary(f.getAssignedOfficer()), f.getActivityType(), f.getScheduledAt(),
                f.getStatus(), f.isOverdue(now), f.getNotes(), f.getOutcome(), f.getCompletedAt(), f.getCreatedAt(),
                canEdit);
    }
}
