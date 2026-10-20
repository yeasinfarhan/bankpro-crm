package com.bankpro.mapper;

import com.bankpro.dto.CustomerResponse;
import com.bankpro.dto.UserSummary;
import com.bankpro.entity.AppUser;
import com.bankpro.entity.Customer;
import com.bankpro.entity.CustomerProduct;

import java.util.List;

public final class CustomerMapper {

    private CustomerMapper() {
    }

    /** "CUS-000042" — formatted from the id, so it can never get out of sync. */
    public static String code(Long id) {
        return id == null ? null : String.format("CUS-%06d", id);
    }

    public static CustomerResponse toResponse(Customer c, List<CustomerProduct> holdings,
                                              CustomerResponse.Permissions permissions) {
        List<CustomerResponse.HeldProduct> products = holdings == null ? null : holdings.stream()
                .map(h -> new CustomerResponse.HeldProduct(h.getProduct().getId(), h.getProduct().getCode(),
                        h.getProduct().getName(), h.getProduct().getCategory().name(),
                        h.getSinceDate().toString(), !h.isManual()))
                .toList();
        return new CustomerResponse(c.getId(), code(c.getId()), c.getFullName(), c.getEmail(), c.getPhone(),
                c.getOccupation(), c.getEmploymentType(), c.getIncomeRange(), c.getCity(), c.getSegment(),
                c.getStatus(), UserMapper.toResponse(c.getBranch()), summary(c.getAssignedOfficer()),
                c.getLastInteractionAt(), c.getCreatedAt(), c.getUpdatedAt(), c.getVersion(), products, permissions);
    }

    public static UserSummary summary(AppUser u) {
        return u == null ? null : new UserSummary(u.getId(), u.getFullName(), u.getJobTitle());
    }
}
